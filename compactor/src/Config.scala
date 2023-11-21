import cats.syntax.all.*
import com.github.eikek.calev.CalEvent
import org.http4s.Uri
import org.http4s.implicits.*

final case class Config(adminApi: Uri, admin: Admin, schedule: CalEvent)

object Config:

  private val compactingScheduleEnv = "COMPACTING_SCHEDULE"
  val shiroIniLocationEnv           = "SHIRO_INI_LOCATION"
  val adminUserEnv                  = "ADMIN_USER"
  val adminUri                      = uri"http://localhost:3030/$$"

  def readConfig: Either[String, Config] =
    (Admin.readConfig -> readCompactingSchedule)
      .mapN(Config(adminUri, _, _))

  private lazy val readCompactingSchedule =
    sys.env
      .get(compactingScheduleEnv)
      .toRight(s"'$compactingScheduleEnv' environment variable given")
      .flatMap(CalEvent.parse)
