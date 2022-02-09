package de.cyproassist.web.snippet.cyprolink

import de.cyproassist.web.util.DCTERMS
import de.cyproassist.web.util.SnippetHelpers._
import net.enilink.komma.core._
import net.enilink.komma.model.{IModel, IModelSet, IObject, IURIConverter}
import net.enilink.komma.model.base.ExtensibleURIConverter
import net.enilink.platform.lift.util.{AjaxHelpers, CurrentContext, Globals}
import net.enilink.vocab.foaf.FOAF
import net.enilink.vocab.rdf.RDF
import net.liftweb.common.{Empty, Full}
import net.liftweb.http.{S, SHtml}
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds.{Noop, Run}
import net.liftweb.util.ClearNodes
import net.liftweb.util.Helpers._

import java.util.{Date, GregorianCalendar, HashMap}
import javax.xml.datatype.{DatatypeFactory, XMLGregorianCalendar}
import scala.collection.immutable.Nil
import scala.jdk.CollectionConverters._

object ProjectHelpers {
  val PROJECTS_MODEL_URI = URIs.createURI("http://cyproassist.de/models/projects")

  def create(model: IModel, uri: URI, name: String): IObject = {
    val projectType = FOAF.TYPE_PROJECT
    val project = model.resolve(uri)
    project.addProperty(RDF.PROPERTY_TYPE, projectType)
    project.setRdfsLabel(name)
    project.set(DCTERMS.PROPERTY_DATE, DatatypeFactory.newInstance.newXMLGregorianCalendar(new GregorianCalendar))
    project
  }

  def loadFromFileSystem(projectsFile: URI) {
    def createModel(modelSet: IModelSet, uri: URI)(initFunc: IModel => Unit): IModel = {
      var model = modelSet.getModel(uri, false)
      if (model == null) {
        model = modelSet.createModel(uri)
        initFunc(model)
        // trigger reloading of model
        model.getManager
      }
      model
    }

    Globals.contextModelSet.vend map { ms =>
      try {
        ms.getUnitOfWork.begin
        createModel(ms, PROJECTS_MODEL_URI) { projectsModel =>
          projectsModel.load(projectsFile, new HashMap)

          val uriConverter = new ExtensibleURIConverter

          val projects = projectsModel.getManager.matchAsserted(null, RDF.PROPERTY_TYPE, FOAF.TYPE_PROJECT).toList
          projects.asScala.map(_.getSubject).foreach { pRef =>
            projectsModel.getManager.matchAsserted(pRef, PROJECTS_MODEL_URI.appendLocalPart("file"), null)
              .toList.asScala.map(_.getObject.asInstanceOf[ILiteral].getLabel).foreach { filePath =>
              val fileUri = URIs.createURI(filePath).resolve(projectsFile)

              val cal = new GregorianCalendar()
              val ts = uriConverter.getAttributes(fileUri, null).get(IURIConverter.ATTRIBUTE_TIME_STAMP).asInstanceOf[Number]
              cal.setTime(new Date(ts.longValue))
              val project = projectsModel.resolve(pRef)
              val newDate = DatatypeFactory.newInstance.newXMLGregorianCalendar(cal)

              // update model if file has changed
              project.get(DCTERMS.PROPERTY_DATE) match {
                case oldDate: XMLGregorianCalendar => {
                  if (newDate.compare(oldDate) > 0) {
                    // delete model to trigger reloading
                    Option(ms.getModel(pRef.getURI, false)) foreach { model =>
                      val changeSupport = ms.getDataChangeSupport
                      try {
                        changeSupport.setEnabled(null, false)
                        model.getManager.clear
                      } finally {
                        changeSupport.setEnabled(null, true)
                      }
                      model.unload
                      ms.getModels.remove(model)
                      ms.getMetaDataManager.remove(model)
                    }
                  }
                }
                case _ => // do nothing
              }
              project.set(DCTERMS.PROPERTY_DATE, newDate)

              createModel(ms, pRef.getURI) { model =>
                model.load(fileUri, new HashMap)
                model.addImport(URIs.createURI("http://linkedfactory.org/vocab/maintenance"), "lf-maint")
              }
            }
          }
        }
      } finally {
        ms.getUnitOfWork.end
      }
    }
  }
}

class Projects {
  /**
   * Creates a canonical project URI in the form of [model name]/projects/[name].
   */
  def projectUri(model: IModel, name: String) = {
    var ns = model.getManager.getNamespace("")
    if (ns.lastSegment == "") ns = ns.trimSegments(1)
    ns.appendSegments(Array("projects", "")).appendLocalPart(name)
  }

  /**
   *
   */
  def create = SHtml.hidden(() => {
    val result = for {
      model <- Globals.contextModel.vend
      name <- doAlert(paramNotEmpty("name", "Bitte einen Namen eingeben."))
    } yield {
      val uri = projectUri(model, name)
      var project = model.resolve(uri)
      val projectType = FOAF.TYPE_PROJECT
      (if (project.getRdfTypes.contains(projectType)) {
        S.error("alert-name", "Ein Projekt mit diesem Namen existiert bereits.")
        Empty
      } else {
        project = ProjectHelpers.create(model, uri, name)
        S.param("description").filter(!_.isEmpty) foreach {
          desc => project.setRdfsComment(desc)
        }

        val projectModel = project.getModel.getModelSet.createModel(project.getURI)
        projectModel.setLoaded(true)
        // add at least one statement to the model
        projectModel.getManager.add(new Statement(project, RDF.PROPERTY_TYPE, projectType))
        // set up the project with imports etc.
        // projectModel.addImport(someModel, null);
        Full(Run(s"$$(document).trigger('project-created', ['${project.getURI}']);"))
      }) openOr Noop
    }
    result openOr Noop
  }: JsCmd)

  /**
   * Delete the complete model that was created for a project.
   * The project is determined by using the current RDFa subject of the HTML node.
   */
  def delete = CurrentContext.value.map { c =>
    AjaxHelpers.onEvents("onclick" :: Nil, _ => {
      val project = c.subject.asInstanceOf[IEntity]
      val t = project.getEntityManager.getTransaction
      val result =
        try {
          for {
            m <- Globals.contextModel.vend
            modelSet = m.getModelSet
            projectModel <- Option(modelSet.getModel(project.getURI, false))
          } {
            projectModel.getManager.clear
            modelSet.getModels.remove(projectModel)
            modelSet.getMetaDataManager.remove(projectModel)
          }
          t.begin
          project.getEntityManager.removeRecursive(project, true)
          t.commit
          Full(Run(s"""$$('[about="${project.getURI}"]').remove()"""))
        } catch {
          case _: Throwable => if (t.isActive) t.rollback; Empty
        }
      result openOr Noop
    }) andThen "a [href]" #> "javascript://"
  } openOr ClearNodes
}