package de.cyproassist.web.snippet.cyprolink

import de.cyproassist.web.util.LF_MAINT
import de.cyproassist.web.util.SnippetHelpers._
import net.enilink.komma.core.URIs
import net.enilink.komma.em.concepts.IResource
import net.enilink.platform.lift.util.{CurrentContext, Globals}
import net.liftweb.common.{Empty, Full}
import net.liftweb.http.{S, SHtml}
import net.liftweb.http.js.JsCmds
import net.liftweb.http.js.JsCmds.Run
import net.liftweb.util.Helpers._

class Machines {
  def create = SHtml.hidden(() => {
    val result = for {
      model <- Globals.contextModel.vend
      name <- doAlert(paramNotEmpty("name", "Bitte einen Namen eingeben."))
      itemType <- doAlert(paramNotEmpty("type", "Bitte einen Typ eingeben.")).map(URIs.createURI(_))
    } yield {
      val em = model.getManager
      val uri = model.getURI.appendLocalPart(URIs.encodeSegment("machine-" + name, true))
      val query = em.createQuery("prefix lf-maint: <" + LF_MAINT.NS_URI + "> " +
        "ask { ?s a [] }").setParameter("s", uri)
      if (query.getBooleanResult) {
        doAlert(failure("nr", "Ein Element mit diesem Namen existiert bereits."))
      } else {
        val item = em.createNamed(uri, itemType).asInstanceOf[IResource]
        S.param("description").filter(!_.isEmpty) foreach {
          desc => item.setRdfsComment(desc)
        }
        Full(Run(s"$$(document).trigger('machine-created', ['$item']);"))
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
}