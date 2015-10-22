package com.github.macobo.checker.server

import java.util

import scala.collection.JavaConverters._
import akka.actor.{Actor, ActorLogging, ActorRef, Stash}
import biz.paluch.spinach.api.sync.DisqueCommands
import biz.paluch.spinach.{DisqueClient, DisqueURI}
import com.github.macobo.checker.server.JobAvailabilityManager.{JobId, MakeAvailable}
import com.github.macobo.checker.server.Serializer._
import com.lambdaworks.redis.cluster.ClusterClientOptions
import spray.json._

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

object Job {
  type spinachJob = biz.paluch.spinach.api.Job[String, String]
  def apply(job: spinachJob): Job =
    Job(job.getBody, job.getId, Some(job.getQueue))

  def get(jobs: util.List[spinachJob]): Option[Job] = {
    jobs.asScala.toList match {
      case Nil => None
      case x::Nil => Some(apply(x))
      case _ => throw new IllegalArgumentException("Tried to construct a job with multiple arguments.")
    }
  }
}
case class Job(message: String, id: JobId, source: Option[String])

// Actor which abstracts away the underlying queue.
// It can both check various queues in a pull-based model as well as add things to the queue.
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

  lazy val client = {
    val client = new DisqueClient(DisqueURI.create(queueHost, queuePort));
    client.setOptions(new ClusterClientOptions.Builder().refreshClusterView(true).build());
    client
  }

  lazy val syncCommands: DisqueCommands[String, String] = client.connect().sync()

  val ackTimeout = 5.seconds.toMillis

  def checkQueue: Receive = {
    case CheckQueue(targetRef) => {
      log.debug(s"Checking queues for new messages. queues=${queues}, targetActor=${targetRef.path}")
      Job.get(syncCommands.getjobs(0, SECONDS, 1, queues: _*)) match {
        case Some(job) => {
          targetRef ! job
          syncCommands.ackjob(job.id)
        }
        case None => {}
      }
    }
  }

  def addJobs: Receive = {
    case MakeAvailable(listing, t) => {
      val jobJson = listing.check.toJson.compactPrint
      // TODO: support check timeouts, delay here.
      val id = syncCommands.addjob(
        projectQueue(listing.check.project),
        jobJson,
        ackTimeout,
        MILLISECONDS
      )

      log.debug(s"Making check available: ${listing}. id=${id}")
      sender ! id
    }
  }

  def enqueue: Receive = {
    case Enqueue(queue, job, _, _) => {
      val jobJson = job.toJson.compactPrint
      log.debug(s"Adding job to ${queue}. job=${jobJson}")
      syncCommands.addjob(queue, jobJson, ackTimeout, MILLISECONDS)
    }
  }

  def runnerMode = checkQueue orElse enqueue
  def serverMode = checkQueue orElse addJobs

  def receive() = queueMode match {
    case ServerMode => serverMode
    case RunnerMode => runnerMode
  }
}
