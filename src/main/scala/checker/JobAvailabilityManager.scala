package checker

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import checker.JobAvailabilityManager._
import macobo.disque.commands.JobId

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Try, Failure, Success}

object JobAvailabilityManager {
  case class JobsAvailable(jobs: Seq[CheckListing])
  case class JobsUnavailable(jobs: Seq[CheckListing])
  case class GetState()

  case class MakeAvailable(check: CheckListing)
  case class DeleteCheck(jobId: JobId)
}

/**
 * ClusterJobManager keeps track of what checks are currently scheduled in the cluster and deals with certain
 * new checks becoming available (or old ones unavailable).
 */
class JobAvailabilityManager(checkQueue: ActorRef)(implicit ec: ExecutionContext)
  extends Actor with ActorLogging {

  var availableChecks = Map.empty[Check, CheckListing]
  var availability = Map.empty[Check, Int]
  var queueIds = Map.empty[Check, JobId]

  def enqueue(check: CheckListing): Future[JobId] = {
    // Note: This piece is _hard_ to distribute without double-scheduling checks.
    implicit val timeout = Timeout(5.seconds)
    (checkQueue ? MakeAvailable(check)).mapTo[JobId]
  }

  def delete(check: CheckListing) =
    queueIds.get(check.check) match {
      case Some(id) =>
        checkQueue ! DeleteCheck(id)
      case None =>
        log.warning(s"Could not make check unavailable, no enqueued checks with that name. check=${check.check}, queueIds=${queueIds}")
    }

  // Makes jobs in the sequence available, using an all-or-nothing strategy.
  def updateAvailability(checks: Seq[CheckListing], delta: Int) = {
    var available = availableChecks
    var counts = availability

    val futures: Seq[Future[(CheckListing, JobId)]] = checks.flatMap { listing =>
      val count = counts.getOrElse(listing.check, 0) + delta
      require(count >= 0, "Tried to remove a non-existing check!")
      counts = counts.updated(listing.check, count)

      if (delta == +1 && count == 1) {
        available = available.updated(listing.check, listing)
        Some(enqueue(listing).map { (listing, _) })
      } else if (count == 0) {
        available = available - listing.check
        counts = counts - listing.check
        delete(listing)
        None
      } else {
        None
      }
    }

    val combined = Future.sequence(futures)
    Try { Await.result(combined, 10.seconds) } match {
      case Success(inserts) => {
        val pairs = inserts.map { p => (p._1.check, p._2) }
        availableChecks = available
        availability = counts
        queueIds = queueIds ++ pairs
        log.info(s"New checks available. checks=${checks.length}, newCheckCount=${inserts.length}, newChecks=${pairs.map(_._1)}")
      }
      case Failure(reason) =>
        log.error(reason, s"Failed to make new checks available. newChecks=${checks}")
    }
  }

  def receive = {
    case JobsAvailable(checks) =>   updateAvailability(checks, +1)
    case JobsUnavailable(checks) => updateAvailability(checks, -1)
    case GetState() => sender() ! availability.toSet
  }
}
