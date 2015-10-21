package com.github.macobo.checker.server

import akka.actor.{Actor, ActorLogging, ActorRef, Stash}
import com.github.macobo.checker.server.JobAvailabilityManager.MakeAvailable
import macobo.disque.DisqueClient
import macobo.disque.commands.Job
import spray.json._
import Serializer._

import scala.concurrent.duration._

case class CheckQueue(forwardTo: ActorRef)
case class Enqueue(queue: String, job: QueueMessage, delayUntil: Option[Long] = None, retry: Option[Long] = None)
case class Initialize(queueType: String)

object QueueCommunicator {
  val RESULTS_QUEUE = "checker:results"
  val CLUSTER_QUEUE = "checker:cluster"

  def projectQueue(project: String) =
    s"checker:project:${project}"

  def hostQueue(host: String) =
    s"checker::runner::${host}"
}

sealed trait QueueMode
case object ServerMode extends QueueMode
case object RunnerMode extends QueueMode

// Actor which can pull messages from the queue and forward them to be properly parsed and managed
class QueueCommunicator(
  queueMode: QueueMode,
  queues: List[String],
  queueHost: String = "localhost",
  queuePort: Int = 7711
) extends Actor
  with ActorLogging
  with Stash
{
  import QueueCommunicator._
  lazy val client =  new DisqueClient(queueHost, queuePort)

  val queueAddTimeout = 5.seconds.toMillis

  def checkQueue: Receive = {
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

  def addJobs: Receive = {
    case MakeAvailable(listing, t) => {
      val jobJson = listing.check.toJson.compactPrint
      // TODO: support check timeouts, delay here.
      val id = client.addJob(projectQueue(listing.check.project), jobJson, queueAddTimeout)
      log.debug(s"Making check available: ${listing}")
      sender ! id
    }
  }

  def enqueue: Receive = {
    case Enqueue(queue, job, _, _) => {
      val jobJson = job.toJson.compactPrint
      log.debug(s"Adding job to ${queue}. job=${jobJson}")
      client.addJob(queue, jobJson, queueAddTimeout)
    }
  }

  def runnerMode = checkQueue orElse enqueue
  def serverMode = checkQueue orElse addJobs

  def receive() = queueMode match {
    case ServerMode => serverMode
    case RunnerMode => runnerMode
  }
}
