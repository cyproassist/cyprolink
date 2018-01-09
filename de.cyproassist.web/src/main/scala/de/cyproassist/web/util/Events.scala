package de.cyproassist.web.util

import net.liftweb.actor.LiftActor
import net.liftweb.http.ListenerManager
import net.enilink.komma.core.IReference

object EventBus extends ListenerManager with LiftActor {
  override def lowPriority = {
    case msg => { sendListenersMessage(msg) }
  }

  def createUpdate = ""
}

case class VuiSubjectChanged(resource: String)
