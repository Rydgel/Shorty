package models

import java.util.UUID
import scala.annotation.tailrec
import scala.util.Random
import play.api.db._
import play.api.Play.current

case class Url(id: UUID, code: String, longUrl: String, hits: Int, created: Long)

object Url {

  import anorm._
  import anorm.SqlParser._

  val url = {
    get[UUID]("id") ~
    get[String]("code") ~
    get[String]("long_url") ~
    get[Int]("hits") ~
    get[Long]("created") map {
      case id~code~longUrl~hits~created => Url(id, code, longUrl, hits, created)
    }
  }

  /**
   * Implicit conversion from UUID to Anorm statement value
   */
  implicit def uuidToStatement = new ToStatement[UUID] {
    def set(s: java.sql.PreparedStatement, index: Int, aValue: UUID): Unit = s.setObject(index, aValue)
  }

  /**
   * Implicit conversion from Anorm row to UUID
   */
  implicit def rowToUUID: Column[UUID] = {
    Column.nonNull[UUID] { (value, meta) =>
      value match {
        case v: UUID => Right(v)
        case _ =>  Left(TypeDoesNotMatch(
          s"Cannot convert $value:${value.asInstanceOf[AnyRef].getClass} to UUID for column ${meta.column}"
        ))
      }
    }
  }

  def all(): List[Url] = DB.withConnection { implicit c =>
    SQL("SELECT * FROM urls ORDER BY created DESC").as(url *)
  }

  def getWithUUID(id: UUID): Option[Url] = DB.withConnection { implicit c =>
    SQL("SELECT * FROM urls WHERE id={id}").on('id -> id).as(url singleOpt)
  }

  def getWithCode(code: String): Option[Url] = DB.withConnection { implicit c =>
    SQL("SELECT * FROM urls WHERE code={code}").on('code -> code).as(url singleOpt)
  }

  def getWithLongUrl(longUrl: String): Option[Url] = DB.withConnection { implicit c =>
    SQL("SELECT * FROM urls WHERE long_url={longUrl}").on('longUrl -> longUrl).as(url singleOpt)
  }

  def create(longUrl: String): Option[UUID] = DB.withConnection { implicit c =>
    SQL("""INSERT INTO urls (id, code, long_url, hits, created)
          | VALUES ({id}, {code}, {long_url}, {hits}, {created})""".stripMargin).on(
        'id -> java.util.UUID.randomUUID, 'code -> getCode,
        'long_url -> longUrl, 'hits -> 0, 'created -> System.currentTimeMillis / 1000
      ).executeInsert(scalar[UUID] singleOpt)
  }

  def createIfNotExists(longUrl: String): UUID = {
    getWithLongUrl(longUrl) map(_.id) getOrElse create(longUrl).get
  }

  def incrementHit(id: UUID) = DB.withConnection { implicit c =>
    SQL("UPDATE urls SET hits = hits + 1 WHERE id={id}").on('id -> id).executeUpdate()
  }

  def delete(id: UUID) = DB.withConnection { implicit c =>
    SQL("DELETE FROM urls where id = {id}").on('id -> id).executeUpdate()
  }

  private def genCode: String = Random.alphanumeric.take(10).mkString

  @tailrec
  private def getCode: String = {
    val code = genCode
    getWithCode(code) match {
      case Some(u: Url) => getCode
      case _ => code
    }
  }

}