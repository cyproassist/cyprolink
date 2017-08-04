package de.cyproassist.web

import java.io.File
import java.io.FileInputStream
import java.net.URL
import java.security.PrivilegedAction
import java.util.HashMap

import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.language.implicitConversions

import org.eclipse.osgi.service.datalocation.Location
import org.osgi.framework.FrameworkUtil

import javax.security.auth.Subject
import net.enilink.komma.core.URIs
import net.enilink.lift.sitemap.HideIfInactive
import net.enilink.lift.sitemap.Menus
import net.enilink.lift.sitemap.Menus.appMenu
import net.enilink.lift.sitemap.Menus.application
import net.enilink.lift.snippet.QueryParams
import net.enilink.lift.util.Globals
import net.liftweb.common.Box
import net.liftweb.common.Full
import net.liftweb.http.GetRequest
import net.liftweb.http.LiftRules
import net.liftweb.http.LiftRulesMocker.toLiftRules
import net.liftweb.http.LiftSession
import net.liftweb.http.RedirectResponse
import net.liftweb.http.Req
import net.liftweb.http.S
import net.liftweb.http.StreamingResponse
import net.liftweb.http.rest.RestHelper
import net.liftweb.sitemap.SiteMap
import net.liftweb.util.Helpers.tryo
import net.enilink.core.security.SecurityUtil

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
    // determine the instance location (workspace)
    val ctx = FrameworkUtil.getBundle(getClass).getBundleContext
    val locService = ctx.getServiceReferences(classOf[Location], Location.INSTANCE_FILTER).headOption.map(ctx.getService(_))

    Subject.doAs(SecurityUtil.SYSTEM_USER_SUBJECT, new PrivilegedAction[Any]() {
      def run = {
        // create default default model on start up if it does not already exist
        Globals.contextModelSet.vend map {
          ms =>
            try {
              ms.getUnitOfWork.begin
              var model = ms.getModel(DEFAULT_MODEL_URI, false)
              if (model == null) {
                model = ms.createModel(DEFAULT_MODEL_URI)
                model.load(URIs.createURI(locService.get.getURL + "etima.ttl"), new HashMap)
                model.addImport(URIs.createURI("http://linkedfactory.org/vocab/maintenance"), "lf-maint")
                // trigger reloading of model
                model.getManager
              }
            } finally {
              ms.getUnitOfWork.end
            }
        }
      }
    })

    Globals.contextModelRules.prepend {
      case Req(`app` :: _, _, _) if !S.param("model").isDefined => Full(DEFAULT_MODEL_URI)
    }

    object ImageDownload extends RestHelper {
      serve {
        case Req("cyprolink" :: "images" :: fileName :: Nil, ext, GetRequest) => for {
          file <- {
            val fileUrl = new URL(locService.get.getURL + "images/" + fileName + "." + ext.toLowerCase)
            Box !! new File(fileUrl.toURI)
          }
          in <- tryo(new FileInputStream(file))
        } yield StreamingResponse(in, () => in.close(), file.length, headers = Nil, cookies = Nil, 200)
      }
    }

    LiftRules.statelessDispatch.append(ImageDownload)

    // Initializes the SPARQL query parameter "currentLang" from the HTTP query parameter "lang" or a default value.
    val setCurrentLang = ((session: LiftSession, req: Req) => {
      req match {
        case Req("cyprolink" :: _, _, _) =>
          val currentLang = S.param("lang") openOr (S.attr("default") openOr "de")
          QueryParams.set(QueryParams.get ++ Map("currentLang" -> currentLang))
        case _ =>
      }
    }: Unit)
    LiftSession.onBeginServicing = setCurrentLang :: LiftSession.onBeginServicing

    // redirect to maintenance app until more content is added
    LiftRules.dispatch.prepend {
      case Req("cyprolink" :: (Nil | "index" :: Nil), _, _) => {
        () => Full(RedirectResponse("/cyprolink/maint/"))
      }
    }
  }

  def shutdown {
  }
}