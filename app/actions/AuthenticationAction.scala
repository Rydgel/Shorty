package actions

import play.api.Play
import play.api.mvc._

import scala.concurrent.Future
import org.apache.commons.codec.binary.Base64.decodeBase64


object AuthenticationAction extends ActionBuilder[Request] {

  private val headers = "WWW-Authenticate" -> "Basic realm=\"whosmad\""
  private val login = Play.current.configuration.getString("auth.username").getOrElse("admin")
  private val password = Play.current.configuration.getString("auth.password").getOrElse("admin")

  private def getUser(request: RequestHeader): Option[List[String]] = {
    request.headers.get("Authorization").flatMap { authorization =>
      authorization.split(" ").drop(1).headOption.flatMap { encoded =>
        new String(decodeBase64(encoded.getBytes)).split(":").toList match {
          case c :: s :: Nil => Some(List(c, s))
          case _ => None
        }
      }
    }
  }

  def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
    getUser(request) match {
      case Some(c: List[String]) if c(0) == login && c(1) == password => block(request)
      case _ => Future.successful(Results.Unauthorized.withHeaders(headers))
    }
  }
}
