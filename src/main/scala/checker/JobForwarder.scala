package checker

import akka.actor.{Actor, ActorLogging}
import macobo.disque.commands.{Job, JobId}
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods

case class Check(project: String, name: String)
case class Host(id: String, knownChecks: Seq[Check])

trait Timestamped {
  def t: Option[Long]
  val timestamp = t getOrElse System.currentTimeMillis()
}

sealed trait QueueMessage extends Timestamped {
  def messageType: String
}

case class ClusterJoin(hostId: String, knownChecks: Seq[Check], t: Option[Long] = None) extends QueueMessage {
  val messageType = "CLUSTERJOIN"
  lazy val host = Host(hostId, knownChecks)
}
case class Heartbeat(hostId: String, t: Option[Long] = None) extends QueueMessage {
  val messageType = "HEARTBEAT"
}
case class CheckResult(t: Option[Long] = None) extends QueueMessage {
  val messageType = "CHECKRESULT"
}

object JobParser {
  implicit val formats = DefaultFormats
  private case class Message(messageType: String)

  def parseMessage(message: String): QueueMessage = {
    val parsed = JsonMethods.parse(message)
    val msgFormat = parsed.extract[Message]
    msgFormat.messageType match {
      case "CHECKRESULT" => parsed.extract[CheckResult]
      case "HEARTBEAT" => parsed.extract[Heartbeat]
      case "CLUSTERJOIN" => parsed.extract[ClusterJoin]
    }
  }
}

// This Actor unpacks messages from their stringified messages and forwards them to appropriate actors
class JobForwarder extends Actor with ActorLogging {
  def resultManager = context.actorSelection("./result_manager")
  def clusterManager = context.actorSelection("./cluster_manager")

  def processQueueMessage(message: String, id: JobId, sourceQueue: Option[String]) = {
    val parsed = JobParser.parseMessage(message)
    log.debug(s"Forwarding message. parsed=${parsed}")
    parsed match {
      case r: CheckResult => resultManager ! r
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
