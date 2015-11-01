package com.github.macobo.checker.server.protocol
import com.github.macobo.checker.server.{CheckResultType, CheckId, Timestamped, CheckListing}

import scala.concurrent.duration.Duration

case class Host(id: String, knownChecks: Seq[CheckListing]) {
  lazy val knownProjects: List[String] =
    knownChecks.map { _.check.project }.toSet.toList.sorted
}

sealed trait QueueMessage extends Timestamped {
  def messageType: String
}

case class RunnerJoin(hostId: String, knownChecks: Seq[CheckListing], t: Option[Long] = None) extends QueueMessage {
  val messageType = "CLUSTERJOIN"
  lazy val host = Host(hostId, knownChecks)
}
case class Heartbeat(hostId: String, t: Option[Long] = None) extends QueueMessage {
  val messageType = "HEARTBEAT"
}

case class CheckResultMessage(
  check: CheckId,
  result: CheckResultType,
  log: String,
  timeTaken: Duration,
  t: Option[Long] = None
) extends QueueMessage {
  val messageType = "CHECKRESULT"
}

