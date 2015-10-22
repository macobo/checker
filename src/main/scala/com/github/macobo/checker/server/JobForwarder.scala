package com.github.macobo.checker.server

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.github.macobo.checker.server.JobAvailabilityManager.JobId
import com.github.macobo.checker.server.Serializer._
import spray.json._

import scala.concurrent.duration.Duration

case class Host(id: String, knownChecks: Seq[CheckListing]) {
  lazy val projects: List[String] =
    knownChecks.map { _.check.project }.toSet.toList.sorted
}

sealed trait QueueMessage extends Timestamped {
  def messageType: String
}

case class ClusterJoin(hostId: String, knownChecks: Seq[CheckListing], t: Option[Long] = None) extends QueueMessage {
  val messageType = "CLUSTERJOIN"
  lazy val host = Host(hostId, knownChecks)
}
case class Heartbeat(hostId: String, t: Option[Long] = None) extends QueueMessage {
  val messageType = "HEARTBEAT"
}

case class CheckResultMessage(
  check: Check,
  result: CheckResultType,
  log: String,
  timeTaken: Duration,
  t: Option[Long] = None
) extends QueueMessage {
  val messageType = "CHECKRESULT"
}

// This Actor unpacks messages from their stringified messages and forwards them to appropriate actors
class JobForwarder(resultManager: ActorRef, clusterManager: ActorRef) extends Actor with ActorLogging {
  def processQueueMessage(message: String, id: JobId, sourceQueue: Option[String]) = {
    val parsed = message.parseJson.convertTo[QueueMessage]
    log.debug(s"Forwarding message. parsed=${parsed}")
    parsed match {
      case m: CheckResultMessage => resultManager ! m
      case m: Heartbeat  => clusterManager ! m
      case m: ClusterJoin => clusterManager ! m
    }
  }

  def receive = {
    case Job(message: String, id, source) => {
      processQueueMessage(message, id, source)
    }
  }
}
