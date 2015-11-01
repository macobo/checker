package com.github.macobo.checker.server

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.github.macobo.checker.server.JobAvailabilityManager.JobId
import com.github.macobo.checker.server.protocol._
import Serializer._
import spray.json._

import scala.concurrent.duration.Duration

// This Actor unpacks messages from their stringified messages and forwards them to appropriate actors
class QueueMessageForwarder(resultManager: ActorRef, clusterManager: ActorRef) extends Actor with ActorLogging {
  def processQueueMessage(message: String, id: JobId, sourceQueue: Option[String]) = {
    val parsed = message.parseJson.convertTo[QueueMessage]
    log.debug(s"Forwarding message. parsed=${parsed}")
    parsed match {
      case m: CheckResultMessage => resultManager ! m
      case m: Heartbeat  => clusterManager ! m
      case m: RunnerJoin => clusterManager ! m
    }
  }

  def receive = {
    case Job(message: String, id, source) => {
      processQueueMessage(message, id, source)
    }
  }
}
