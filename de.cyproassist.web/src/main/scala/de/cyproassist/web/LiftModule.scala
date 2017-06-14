package de.cyproassist.web

import scala.language.implicitConversions
import net.enilink.lift.sitemap.Menus
import net.enilink.lift.sitemap.Menus._
import net.liftweb.http.S
import net.liftweb.sitemap.LocPath.stringToLocPath
import net.liftweb.sitemap.Menu
import net.liftweb.sitemap.SiteMap
import net.enilink.lift.util.Globals
import net.liftweb.common.Full
import net.enilink.komma.core.URIs
import java.util.HashMap
import net.liftweb.http.Req
import net.liftweb.http.LiftRules
import net.enilink.lift.sitemap.HideIfInactive

/**
 * This is the main class of the web module. It sets up and tears down the application.
 */
class LiftModule {
  implicit val app = "cyprolink"

  def sitemapMutator: SiteMap => SiteMap = {
    val menu = application(app, app :: Nil, List(
      appMenu("maint.instructions", S ? "Maintenance", List("maint", "index")) submenus List(
        appMenu("maint.event", S ? "Event", List("maint", "event")) >> HideIfInactive)))

    Menus.sitemapMutator(menu :: Nil)
  }

  val DEFAULT_MODEL_URI = URIs.createURI("http://cyproassist.de/models/maintenance")

  def boot {
    // create default default model on start up if it does not already exist
    Globals.contextModelSet.vend map {
      ms =>
        try {
          ms.getUnitOfWork.begin
          var model = ms.getModel(DEFAULT_MODEL_URI, false)
          if (model == null) {
            model = ms.createModel(DEFAULT_MODEL_URI)
            model.addImport(URIs.createURI("http://linkedfactory.org/vocab/maintenance"), "lf-maint")
          }
        } finally {
          ms.getUnitOfWork.end
        }
    }

    Globals.contextModelRules.prepend {
      case Req(`app` :: _, _, _) if !S.param("model").isDefined => Full(DEFAULT_MODEL_URI)
    }
  }

  def shutdown {
  }
}