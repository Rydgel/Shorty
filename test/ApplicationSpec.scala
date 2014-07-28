import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.Play
import play.api.test._
import play.api.test.Helpers._


object authUtility {

  private def makeDigest: String = {
    val login = Play.current.configuration.getString("auth.username").getOrElse("admin")
    val password = Play.current.configuration.getString("auth.password").getOrElse("admin")
    new sun.misc.BASE64Encoder().encode((login + ":" + password).mkString.getBytes)
  }

  def makeHeader: (String, String) = ("Authorization", s"Basic $makeDigest")
}

@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {

  "Application" should {

    "send 404 on a bad request" in new WithApplication {
      val notFound = route(FakeRequest(GET, "/boum")).get
      status(notFound) must equalTo(NOT_FOUND)
      contentType(notFound) must beSome.which(_ == "text/html")
      contentAsString(notFound) must contain ("404")
    }

    "redirect to list of urls" in new WithApplication {
      val home = route(FakeRequest(GET, "/")).get
      status(home) must equalTo(SEE_OTHER)
    }

    "admin dashboard should be protected by auth" in new WithApplication {
      val dashboard = route(FakeRequest(GET, "/admin")).get
      status(dashboard) must equalTo(UNAUTHORIZED)
      headers(dashboard).get("WWW-Authenticate").get must contain ("whosmad")
    }

    "admin dashboard should unlock with good login and password" in new WithApplication {
      val dashboard = route(FakeRequest(GET, "/admin").withHeaders(authUtility.makeHeader)).get
      status(dashboard) must equalTo(OK)
      contentType(dashboard) must beSome.which(_ == "text/html")
      contentAsString(dashboard) must contain ("Shitty URL shorter")
    }
  }
}
