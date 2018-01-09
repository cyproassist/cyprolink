package de.cyproassist.web.vui;

import net.liftweb.http.GetRequest
import java.io.FileInputStream
import net.liftweb.http.Req
import java.io.File
import net.liftweb.http.StreamingResponse
import net.liftweb.http.rest.RestHelper
import net.liftweb.common.Box
import java.security.Policy.Parameters
import net.liftweb.http.OkResponse
import net.liftweb.http.S
import de.cyproassist.web.util.VuiSubjectChanged
import net.liftweb.http.InMemoryResponse
import de.cyproassist.web.util.EventBus
import net.liftweb.util.Helpers._

object VuiFeedback extends RestHelper {
  val CORS_HEADERS = ("Access-Control-Allow-Origin", "*") :: ("Access-Control-Allow-Credentials", "true") :: //
    ("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS") :: //
    ("Access-Control-Allow-Headers", "WWW-Authenticate,Keep-Alive,User-Agent,X-Requested-With,Cache-Control,Content-Type") :: Nil

  def responseHeaders = CORS_HEADERS ::: S.getResponseHeaders(Nil)

  serve {
    case "cyprolink" :: "vui" :: "feedback" :: Nil Post req =>
      req.json.map { json =>
        val s = (json \\ "item").values.toString
        EventBus ! VuiSubjectChanged(s)
      }

      InMemoryResponse(Array(), responseHeaders, S.responseCookies, 200)
  }
}