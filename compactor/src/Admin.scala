import cats.syntax.all.*
import org.apache.shiro.config.Ini
import org.http4s.BasicCredentials
import org.http4s.headers.Authorization

final case class Admin(username: String, password: String):
  lazy val asAuthHeader = Authorization(BasicCredentials(username, password))

object Admin:

  def readConfig: Either[String, Admin] =
    sys.env
      .get(Config.adminUserEnv)
      .toRight(s"No '${Config.adminUserEnv}' environment variable given")
      .flatMap(username => readAdminPass(username).map(Admin.apply(username, _)))

  private def readAdminPass(adminUserName: String): Either[String, String] =
    val shiroFileName = "shiro.ini"
    sys.env
      .get(Config.shiroIniLocationEnv)
      .toRight(s"'${Config.shiroIniLocationEnv}' environment variable given")
      .flatMap(path =>
        Either
          .catchNonFatal(Ini.fromResourcePath(path))
          .leftMap(err => s"$shiroFileName couldn't be read from '$path': ${err.getMessage}")
      )
      .flatMap(ini => Option(ini.getSection("users")).toRight(s"No 'users' section in $shiroFileName"))
      .flatMap(users => Option(users.get(adminUserName)).toRight(s"No 'adminUserName' in $shiroFileName"))
