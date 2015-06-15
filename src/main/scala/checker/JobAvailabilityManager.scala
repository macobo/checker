package checker

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import checker.JobAvailabilityManager.{JobsAvailable, MakeAvailable}
import macobo.disque.commands.JobId

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

trait CheckJob {
  def queueName: String
  // Typically a combination of project-name
  def identifier: String
}

object JobAvailabilityManager {
  case class JobsAvailable(jobs: Seq[CheckJob])
  case class JobsUnavailable(jobs: Seq[CheckJob])

  case class MakeAvailable(check: CheckJob)
  case class DeleteCheck(check: CheckJob)
}

/**
 * ClusterJobManager keeps track of what checks are currently scheduled in the cluster and deals with certain
 * new checks becoming available (or old ones unavailable).
 *
 *
 */
class JobAvailabilityManager(implicit ec: ExecutionContext) extends Actor with ActorLogging {
  var jobAvailability: Map[String, Int] = Map.empty
  var jobs: Map[String, CheckJob] = Map.empty

  def jobQueue: ActorRef = ???

  private def makeAvailable(check: CheckJob): Future[(String, JobId)] = {
    implicit val timeout = Timeout(30.seconds)
    val idFuture: Future[JobId] = (jobQueue ? MakeAvailable(check)).mapTo[JobId]
    idFuture.map { (check.identifier, _) }
  }

  def receive = {
    case JobsAvailable(jobs) => {}
  }
}
