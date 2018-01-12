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

import de.cyproassist.web.snippet.cyprolink.ProjectHelpers
import de.cyproassist.web.vui.VuiFeedback
import javax.security.auth.Subject
import net.enilink.core.security.SecurityUtil
import net.enilink.komma.core.URI
import net.enilink.komma.core.URIs
import net.enilink.komma.model.IModel
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
import net.liftweb.sitemap.Loc.EarlyResponse
import net.liftweb.sitemap.Loc.Hidden
import net.liftweb.sitemap.Loc.If
import net.liftweb.sitemap.Loc.strToFailMsg
import net.liftweb.sitemap.SiteMap
import net.liftweb.util.Helpers.tryo
import net.liftweb.sitemap.Loc.QueryParameters

/**
 * This is the main class of the web module. It sets up and tears down the application.
 */
class LiftModule {
  implicit val app = "cyprolink"

  def sitemapMutator: SiteMap => SiteMap = {
    val redirectTo = EarlyResponse(() => {
      val params = S.param("model").map(m => s"?model=$m").openOr("")
      Full(RedirectResponse(s"/$app/maint/machines$params"))
    })
    def modelParams(includeResource: Boolean) = QueryParameters(() => {
      S.param("model").map(m => ("model", m)).toList ++ (if (includeResource) S.param("resource").map(m => ("resource", m)).toList else Nil)
    })
    val menu = application(app, app :: Nil, List(
      appMenu("maint.redirect1", S ? "", List("maint")) >> Hidden >> redirectTo,
      appMenu("maint.redirect2", S ? "", List("maint", "index")) >> Hidden >> redirectTo,

      appMenu("projects", S ? "Projects", List("projects")),
      appMenu("maint.machines", S ? "Maintenance", List("maint", "machines")) >> If(() => S.param("model").isDefined, "A model is required") >> modelParams(false) submenus List(
        appMenu("maint.events", S ? "Events", List("maint", "events")) >> modelParams(false) submenus List(
          appMenu("maint.event", S ? "Event", List("maint", "event")) >> modelParams(true) >> HideIfInactive))))

    Menus.sitemapMutator(menu :: Nil)
  }

  def boot {
    // determine the instance location (workspace)
    val ctx = FrameworkUtil.getBundle(getClass).getBundleContext
    val locService = ctx.getServiceReferences(classOf[Location], Location.INSTANCE_FILTER).headOption.map(ctx.getService(_))

    Subject.doAs(SecurityUtil.SYSTEM_USER_SUBJECT, new PrivilegedAction[Any]() {
      def run = {
        // create projects on startup
        ProjectHelpers.loadFromFileSystem(URIs.createURI(locService.get.getURL + "projects.ttl"))
      }
    })

    Globals.contextModelRules.prepend {
      case Req(`app` :: "projects" :: _, _, _) => Full(ProjectHelpers.PROJECTS_MODEL_URI)
    }

    object ImageDownload extends RestHelper {
      lazy val nameMapping: Map[String, String] = {
        val mappingFile = new File(new URL(locService.get.getURL + "images/mappings.txt").toURI)
        if (mappingFile.exists) {
          io.Source.fromFile(mappingFile).getLines.map { line =>
            val Array(key, value, _*) = line.split("\\t")
            println(key.trim.toLowerCase, value.trim)
            (key.trim.toLowerCase, value.trim)
          }.toMap
        } else {
          Map.empty
        }
      }

      serve {
        case Req("cyprolink" :: "images" :: fileName :: Nil, ext, GetRequest) => for {
          file <- {
            var nameWithExt = fileName + "." + ext
            nameWithExt = nameMapping.getOrElse(nameWithExt.toLowerCase, nameWithExt)
            val fileUrl = new URL(locService.get.getURL + "images/" + nameWithExt)
            Box !! new File(fileUrl.toURI)
          }
          in <- tryo(new FileInputStream(file))
        } yield StreamingResponse(in, () => in.close(), file.length, headers = Nil, cookies = Nil, 200)
      }
    }

    LiftRules.statelessDispatch.append(ImageDownload)

    // endpoint for voice user interface feedback
    LiftRules.statelessDispatch.append(VuiFeedback)

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
    LiftRules.statelessDispatch.prepend {
      case Req("cyprolink" :: (Nil | "index" :: Nil), _, _) => {
        () => Full(RedirectResponse("/cyprolink/projects"))
      }
    }
  }

  def shutdown {
  }
}