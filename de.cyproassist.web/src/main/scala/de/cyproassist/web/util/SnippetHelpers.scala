package de.cyproassist.web.util

import net.enilink.komma.core.URI
import net.liftweb.common.Box
import net.liftweb.common.Failure
import net.liftweb.http.S

/**
 * Provides basic functions that can be used in Lift snippets.
 */
object SnippetHelpers {
  /**
   * Convert an URI instance to a String.
   */
  implicit def uriToStr(uri: URI) = uri.toString

  /**
   * Returns a failure with the give <code>message</code> if the HTTP parameter with the given <code>name</code> is empty.
   */
  def paramNotEmpty(name: String, msg: String) = S.param(name).map(_.trim).filterMsg(name + ":" + msg)(!_.isEmpty)

  /**
   * Tests if the given <code>box</code> box is a failed and then creates an error message for a specific input
   * field if the <code>box</box> has a message that matches the pattern "[name]: [message]"
   * or as global error message.
   */
  def doAlert[T](box: Box[T]) = box match {
    case f @ Failure(msg, _, _) =>
      msg.split(":", 2) match {
        case Array(name, alert) => S.error("alert-" + name, alert)
        case _ => S.error(msg)
      }; f
    case n => n
  }

  /**
   * Creates a failure with a message in the form of "[name]: [message]".
   */
  def failure(name: String, msg: String) = Failure(name + ":" + msg)
}