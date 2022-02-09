package de.cyproassist.web.snippet.cyprolink

import de.cyproassist.web.util.{DCTERMS, LF_MAINT}
import de.cyproassist.web.util.SnippetHelpers._
import net.enilink.komma.core.URIs
import net.enilink.komma.em.concepts.IResource
import net.enilink.platform.lift.util.{CurrentContext, Globals}
import net.liftweb.common.{Empty, Full}
import net.liftweb.http.{S, SHtml}
import net.liftweb.http.js.JsCmds
import net.liftweb.http.js.JsCmds.Run
import net.liftweb.util.Helpers._

import java.util.GregorianCalendar
import javax.xml.datatype.DatatypeFactory
import scala.xml.{Elem, NodeSeq}

class Events {
  def create = SHtml.hidden(() => {
    val result = for {
      model <- Globals.contextModel.vend
      nr <- doAlert(paramNotEmpty("nr", "Bitte eine Nummer eingeben.") flatMap (nr => tryo(nr.toInt)))
      eventType <- doAlert(paramNotEmpty("type", "Bitte einen Typ eingeben.")).map(URIs.createURI(_))
      location <- doAlert(paramNotEmpty("location", "Bitte eine Anlage auswählen.")).map(URIs.createURI(_))
    } yield {
      val em = model.getManager
      val query = em.createQuery("prefix lf-maint: <" + LF_MAINT.NS_URI + "> " +
        "ask { ?s a ?type; lf-maint:nr ?nr }").setParameter("type", eventType).setParameter("nr", nr)
      if (query.getBooleanResult) {
        doAlert(failure("nr", "Ein Element mit diesem Typ und dieser Nummer existiert bereits."))
      } else {
        val uri = model.getURI.appendLocalPart("event-" + eventType.localPart() + "-" + nr)
        val event = em.createNamed(uri, eventType).asInstanceOf[IResource]
        event.addProperty(LF_MAINT.PROPERTY_NR, nr)
        S.param("description").filter(!_.isEmpty) foreach {
          desc => event.setRdfsComment(desc)
        }
        event.set(LF_MAINT.PROPERTY_OCCURSINMACHINE, location)
        event.set(DCTERMS.PROPERTY_DATE, DatatypeFactory.newInstance.newXMLGregorianCalendar(new GregorianCalendar))
        Full(Run(s"$$(document).trigger('event-created', ['$event']);"))
      }
    }
    (result openOr Empty) openOr JsCmds.Noop
  })

  def delete = "^ [onclick]" #> {
    val rbox = CurrentContext.value.map { ctx => ctx.subject }
    SHtml.onEvent { s =>
      (for {
        model <- Globals.contextModel.vend
        resource <- rbox
      } yield {
        model.getManager.removeRecursive(resource, true)
        Run(s"""$$('[about="$resource"]').remove()""")
      }) openOr JsCmds.Noop
    }
  }

  /**
   * Gives RDFa variables in an XML snippet a unique name.
   */
  def uniquifyVars(ns: NodeSeq) = {
    val mapping = scala.collection.mutable.Map.empty[String, String]
    def map(name: String) = mapping.getOrElseUpdate(name, name + "_" + nextFuncName)

    def renameRecursive(ns: NodeSeq): NodeSeq = ns.flatMap {
      case e: Elem => {
        val renamed = List("about", "resource").foldLeft(e) {
          case (e, attr) =>
            val value = e \@ attr
            if (value.startsWith("?")) {
              e % (attr -> map(value))
            } else e
        }
        renamed.copy(child = renameRecursive(e.child))
      }
      case o => o
    }

    renameRecursive(ns)
  }

  /**
   * Fill RDFa template from property list given by data-properties.
   */
  def listProperties = ".properties" #> ((pNode: NodeSeq) => pNode match {
    case elem: Elem =>
      val props = (pNode \@ "data-properties").split("\\s+").toSeq
      val newChild = props.flatMap { p =>
        uniquifyVars((".property [about]" #> p &
          ".property-value" #> {
            "^ [rel]" #> p andThen "^ [class+]" #> "editable keep optional"
          }).apply(elem.child))
      }
      elem.copy(child = newChild)
  })
}