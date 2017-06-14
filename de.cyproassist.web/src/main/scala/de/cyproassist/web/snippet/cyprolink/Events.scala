package de.cyproassist.web.snippet.cyprolink

import java.util.GregorianCalendar
import de.cyproassist.web.util.SnippetHelpers._
import javax.xml.datatype.DatatypeFactory
import net.enilink.komma.em.concepts.IResource
import net.enilink.lift.util.CurrentContext
import net.enilink.lift.util.Globals
import net.liftweb.common.Empty
import net.liftweb.common.Full
import net.liftweb.http.S
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmds
import net.liftweb.http.js.JsCmds.Run
import net.liftweb.util.Helpers.strToCssBindPromoter
import net.enilink.komma.core.URIs
import de.cyproassist.web.util.DCTERMS
import de.cyproassist.web.util.LF_MAINT

class Events {
  def create = SHtml.hidden(() => {
    val result = for {
      model <- Globals.contextModel.vend
      nr <- doAlert(paramNotEmpty("nr", "Bitte eine Nummer eingeben."))
      eventType <- doAlert(paramNotEmpty("type", "Bitte einen Typ eingeben.")).map(URIs.createURI(_))
    } yield {
      val em = model.getManager
      val uri = model.getURI.appendLocalPart("event-" + nr)
      if (em.hasMatch(uri, null, null)) {
        doAlert(failure("name", "Ein Element mit diesem Typ und dieser Nummer existiert bereits."))
      } else {
        val event = em.createNamed(uri, eventType).asInstanceOf[IResource]
        S.param("description").filter(!_.isEmpty) foreach {
          desc => event.setRdfsComment(desc)
        }
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
}