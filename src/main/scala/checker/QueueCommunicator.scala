package checker

import akka.actor.{ActorRef, ActorLogging, Actor}
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
  }

  def receive() = {
    case CheckQueue(target) => {
      log.debug(s"Checking queues for new messages. queues=${queues}, targetActor=${target.path}")
      client.getJobMulti(queues, Some(10)) match {
        case Some(job: Job[String]) => {
          target ! job
          client.acknowledge(job.id)
        }
        case None => {}
      }
    }
  }
}
