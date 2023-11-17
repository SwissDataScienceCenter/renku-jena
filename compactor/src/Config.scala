import cats.syntax.all.*

final case class Admin(username: String, password: String)

object Admin:

  private val adminUserEnv = "ADMIN_USER"
  private val adminPassEnv = "ADMIN_PASS"

  def fromConfig: Option[Admin] =
    (sys.env.get(adminUserEnv) -> sys.env.get(adminPassEnv))
      .mapN(Admin.apply)


final case class Config(admin: Admin)

object Config:
  def fromConfig: Option[Config] =
    Admin.fromConfig.map(Config(_))

