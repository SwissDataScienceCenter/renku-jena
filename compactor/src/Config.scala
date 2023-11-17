import cats.syntax.all.*
import org.http4s.headers.Authorization
import org.http4s.implicits.*
import org.http4s.{BasicCredentials, Uri}

final case class Admin(username: String, password: String):
  lazy val asAuthHeader = Authorization(BasicCredentials(username, password))

object Admin:

  private val adminUserEnv = "ADMIN_USER"
  private val adminPassEnv = "ADMIN_PASS"

  def fromConfig: Option[Admin] =
    (sys.env.get(adminUserEnv) -> sys.env.get(adminPassEnv))
      .mapN(Admin.apply)

final case class Config(adminApi: Uri, admin: Admin)

object Config:

  val adminUri = uri"http://localhost:3030/$$"

  def fromConfig: Option[Config] =
    Admin.fromConfig.map(Config(adminUri, _))
