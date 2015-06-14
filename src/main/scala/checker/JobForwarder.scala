package checker

import akka.actor.{Actor, ActorLogging}
import macobo.disque.commands.{Job, JobId}
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods

case class Check(project: String, name: String)

sealed trait QueueMessage {
  def messageType: String
}
case class ClusterJoin(hostId: String, knownChecks: Seq[Check]) extends QueueMessage {
  val messageType = "CLUSTERJOIN"
}
case class Heartbeat(hostId: String) extends QueueMessage {
  val messageType = "HEARTBEAT"
}
case class CheckResult() extends QueueMessage {
  val messageType = "CHECKRESULT"
}

// This Actor unpacks messages from their stringified messages and forwards them to appropriate actors
class JobForwarder extends Actor with ActorLogging {

  implicit val formats = DefaultFormats
  private case class Message(messageType: String)

  def resultManager = context.actorSelection("../result_manager")
  def clusterManager = context.actorSelection("../cluster_manager") 

  def parseMessage(message: String): QueueMessage = {
    val parsed = JsonMethods.parse(message)
    val msgFormat = parsed.extract[Message]
    msgFormat.messageType match {
      case "CHECKRESULT" => parsed.extract[CheckResult]
      case "HEARTBEAT" => parsed.extract[Heartbeat]
      case "CLUSTERJOIN" => parsed.extract[ClusterJoin]
    }
  }

  def processQueueMessage(message: String, id: JobId, sourceQueue: Option[String]) = {
    parseMessage(message) match {
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
