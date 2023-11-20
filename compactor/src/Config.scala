import cats.syntax.all.*
import com.github.eikek.calev.CalEvent
import org.http4s.headers.Authorization
import org.http4s.implicits.*
import org.http4s.{BasicCredentials, Uri}

final case class Config(adminApi: Uri, admin: Admin, schedule: CalEvent)

object Config:

  private val compactingScheduleEnv = "COMPACTING_SCHEDULE"
  val adminUserEnv                  = "ADMIN_USER"
  val adminPassEnv                  = "ADMIN_PASS"
  val adminUri                      = uri"http://localhost:3030/$$"

  def readConfig: Either[String, Config] =
    (Admin.readConfig -> readCompactingSchedule)
      .mapN(Config(adminUri, _, _))

  private lazy val readCompactingSchedule =
    sys.env
      .get(compactingScheduleEnv)
      .toRight(s"'$compactingScheduleEnv' not found")
      .flatMap(CalEvent.parse)

final case class Admin(username: String, password: String):
  lazy val asAuthHeader = Authorization(BasicCredentials(username, password))

object Admin:

  def readConfig: Either[String, Admin] =
    (sys.env.get(Config.adminUserEnv) -> sys.env.get(Config.adminPassEnv))
      .mapN(Admin.apply)
      .toRight("Admin credentials not found")
