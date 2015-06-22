package com.github.macobo.checker.server

import akka.actor.{Actor, ActorLogging, ActorRef}
import macobo.disque.commands.{Job, JobId}
import org.json4s.FieldSerializer._
import org.json4s.JsonAST.{JBool, JField, JObject, JString}
import org.json4s.native.{JsonMethods, Serialization}
import org.json4s.{CustomSerializer, DefaultFormats, FieldSerializer}

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
  timeTaken: Int,
  t: Option[Long] = None
) extends QueueMessage {
  val messageType = "CHECKRESULT"
}

object JobParser {
  val fieldSerializer = FieldSerializer[QueueMessage](ignore("timestamp"))

  class ResultSerializer extends CustomSerializer[CheckResultType](format => ({
    case JObject(JField("success", JBool(true)) :: Nil) => CheckSuccess()
    case JObject(JField("success", JBool(false)) :: JField("reason", JString(reason)) :: Nil) =>
      CheckFailure(reason)
  }, {
    case x: CheckResultType => {
      val firstField = JField("success", JBool(x.success))
      val rest = x match {
        case CheckSuccess() => Nil
        case CheckFailure(reason) => JField("reason", JString(reason)) :: Nil
      }
      JObject(firstField :: rest)
    }
  }))

  implicit val formats = DefaultFormats + fieldSerializer + new ResultSerializer

  private case class Message(messageType: String)

  def parseMessage(message: String): QueueMessage = {
    val parsed = JsonMethods.parse(message)
    val msgFormat = parsed.extract[Message]
    msgFormat.messageType match {
      case "CHECKRESULT" => parsed.extract[CheckResultMessage]
      case "HEARTBEAT" => parsed.extract[Heartbeat]
      case "CLUSTERJOIN" => parsed.extract[ClusterJoin]
    }
  }

  def jsonify(message: QueueMessage): String = {
    Serialization.write(message)
  }
}

// This Actor unpacks messages from their stringified messages and forwards them to appropriate actors
class JobForwarder(resultManager: ActorRef, clusterManager: ActorRef) extends Actor with ActorLogging {
  def processQueueMessage(message: String, id: JobId, sourceQueue: Option[String]) = {
    val parsed = JobParser.parseMessage(message)
    log.debug(s"Forwarding message. parsed=${parsed}")
    parsed match {
      case r: CheckResultMessage => resultManager ! r
      case hb: Heartbeat  => clusterManager ! hb
      case m: ClusterJoin => clusterManager ! m
    }
  }

  def receive = {
    case Job(message: String, id, source) => {
      processQueueMessage(message, id, source)

    }
  }
}
