package controllers

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import java.util.UUID
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._

import actions.AuthenticationAction
import models.Url


object Application extends Controller {

  private val urlRegex = """^(https?:\/\/)([\da-z\.-]+)\.([a-z\.]{2,6})([\/\w \.-]*)*\/?$""".r
  private val isValidUrl: String => Boolean = { case s: String => urlRegex.unapplySeq(s).isDefined }
  private val urlForm = Form(
    single("long_url" -> text).verifying("is a valid URL", isValidUrl)
  )

  def index = Action { implicit request =>
    Redirect(routes.Application.getAllUrls)
  }

  def getAllUrls = AuthenticationAction { implicit request =>
    val urls = Url.all()
    Ok(views.html.index(urls))
  }

  def createUrl = AuthenticationAction { implicit request =>
    urlForm.bindFromRequest.fold(
      formWithErrors =>
        Redirect(routes.Application.getAllUrls).flashing("error" -> "I need a valid URL")
      , urlData => {
        val uuid = Url.createIfNotExists(urlData)
        Redirect(routes.Application.getUrl(uuid)).flashing("success" -> "Url has been created")
      }
    )
  }

  def getUrl(id: UUID) = AuthenticationAction { implicit request =>
    Url.getWithUUID(id) map { url =>
      Ok(views.html.index(List(url), detail = true))
    } getOrElse NotFound(views.html.notFound())
  }

  def deleteUrl(id: UUID) = AuthenticationAction { implicit request =>
    Url.delete(id)
    Redirect(routes.Application.getAllUrls).flashing("success" -> "Url has been deleted")
  }

  def getRedirectWithCode(code: String) = Action { implicit request =>
    Url.getWithCode(code) map { url =>
      // no need to wait for that
      Future { Url.incrementHit(url.id) }
      Redirect(url.longUrl)
    } getOrElse NotFound(views.html.notFound())
  }

}