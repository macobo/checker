package com.github.macobo.checker.server

import com.github.nscala_time.time.Imports._
import org.joda.time.LocalDate

import scala.concurrent.duration.Duration

trait Timestamped {
  def t: Option[Long]
  val timestamp = t getOrElse System.currentTimeMillis()
}

case class CheckId(project: String, name: String) {
  def identifier = s"${project}::${name}"
}

case class CheckListing(
  check: CheckId,
  runsEvery: Duration,
  timelimit: Duration
) {
  require(runsEvery > timelimit, "Check should not run more frequently than the time limit.")

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
