package checker

import org.joda.time.LocalDate
import com.github.nscala_time.time.Imports._

import scala.concurrent.duration.Duration

trait Timestamped {
  def t: Option[Long]
  val timestamp = t getOrElse System.currentTimeMillis()
}

case class Check(project: String, name: String) {
  def identifier = s"${project}::${name}"
}

case class CheckListing(
  check: Check,
  runsEvery: Duration,
  timelimit: Duration
) {

  def queueTimeout = timelimit * 1.5

  def nextRun(time: Long): Long = {
    val currentDay = new LocalDate(time).toDateTimeAtStartOfDay
    val timesRan = (time - currentDay.getMillis) / runsEvery.toMillis
    // Note: test if the diff exactly divides!
    val nextDate = currentDay + ((timesRan + 1) * runsEvery.toMillis).toDuration

    nextDate.getMillis
  }
}

sealed trait CheckResultType {
  def success: Boolean
}
case class CheckSuccess() extends CheckResultType               { val success = true }
case class CheckFailure(reason: String) extends CheckResultType { val success = false }
