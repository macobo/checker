package checker

import akka.actor.{Actor, ActorLogging}
import checker.JobAvailabilityManager.JobsAvailable

import scala.concurrent.ExecutionContext

object JobAvailabilityManager {
  case class JobsAvailable(jobs: Seq[CheckListing])
  case class JobsUnavailable(jobs: Seq[CheckListing])

  case class MakeAvailable(check: CheckListing)
  case class DeleteCheck(check: CheckListing)
}

/**
 * ClusterJobManager keeps track of what checks are currently scheduled in the cluster and deals with certain
 * new checks becoming available (or old ones unavailable).
 *
 *
 */
class JobAvailabilityManager(implicit ec: ExecutionContext) extends Actor with ActorLogging {
//  var jobAvailability: Map[String, Int] = Map.empty
//  var jobs: Map[String, CheckListing] = Map.empty

//  def jobQueue: ActorRef = ???

//  private def makeAvailable(check: CheckJob): Future[(String, JobId)] = {
//    implicit val timeout = Timeout(30.seconds)
//    val idFuture: Future[JobId] = (jobQueue ? MakeAvailable(check)).mapTo[JobId]
//    idFuture.map { (check.identifier, _) }
//  }

  def receive = {
    case JobsAvailable(jobs) => {}
  }
}
