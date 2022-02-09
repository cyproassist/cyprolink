package de.cyproassist.web.comet

import de.cyproassist.web.util.EventBus
import de.cyproassist.web.util.VuiSubjectChanged
import net.liftweb.common.Full
import net.liftweb.http.CometListener
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmds.jsExpToJsCmd
import net.liftweb.json.JsonAST.{ render => renderJson, compactRender }
import net.liftweb.json.JsonDSL.pair2jvalue
import net.liftweb.json.JsonDSL.string2jvalue
import net.liftweb.util.Helpers.intToTimeSpanBuilder

class VuiFeedbackComet extends CometListener with ActorHelper {
  var vuiUserId: String = _

  def registerWith = EventBus

  implicit val formats = net.liftweb.json.DefaultFormats

  override def dontCacheRendering = true

  override def lifespan = Full(5.second)

  override def render = {
    <span></span>
  }

  override def localSetup {
    super.localSetup
    // allow to specify a user ID for future filtering of events
    attributes.get("userid").foreach { v => vuiUserId = v.toString }
  }

  override def localShutdown {
    super.localShutdown
  }

  override def lowPriority: PartialFunction[Any, Unit] = {
    case VuiSubjectChanged(item) => {
      val data = ("item", item)
      //trigger device-registered to all listeners
      partialUpdate(triggerCmd("vui-subject-changed", compactRender(data)))
    }
  }
}

trait ActorHelper {
  def triggerCmd(event: String, data: String): net.liftweb.http.js.JsCmd = {
    JsRaw("""$(document).trigger('""" + event + """', [""" + data + """])""")
  }
}