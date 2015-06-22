package com.github.macobo.checker.server

import akka.actor.{Actor, ActorLogging, ActorRef}
import macobo.disque.DisqueClient
import macobo.disque.commands.Job

case class CheckQueue(forwardTo: ActorRef)

// Actor which can pull messages from the queue and forward them to be properly parsed and managed
class QueueCommunicator(
  queues: List[String],
  queueHost: String = "localhost",
  queuePort: Int = 7711
) extends Actor
  with ActorLogging
{
  var client: DisqueClient = null

  override def preStart() = {
    client = new DisqueClient(queueHost, queuePort)
    log.info("connected!")
  }

  def receive() = {
    case CheckQueue(targetRef) => {
      log.debug(s"Checking queues for new messages. queues=${queues}, targetActor=${targetRef.path}")
      client.getJobMulti(queues, Some(10)) match {
        case Some(job: Job[String]) => {
          targetRef ! job
          client.acknowledge(job.id)
        }
        case None => {}
      }
    }
  }
}
