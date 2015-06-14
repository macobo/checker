//package main.scala.checker
//
//import akka.actor.{Actor, ActorLogging, ActorRef}
//import akka.pattern.ask
//import akka.util.Timeout
//import checker.Job
//import main.scala.checker.ClusterManager.{ClusterJoin, Heartbeat, UpdateLiveliness}
//import main.scala.checker.JobAvailabilityManager.{AddJob, DeleteJob, JobsAvailable, JobsUnavailable}
//
//import scala.concurrent.duration._
//import scala.concurrent.{Await, Future}
//import scala.util.{Failure, Success}
//
//// Clustermanager receives a message each time a checker joins the cluster and is responsible for removing hosts
//// who are inactive.
//object ClusterManager {
//  // Received messages
//  case class ClusterJoin(host: AHost, timestamp: Long)
//  case class Heartbeat(hostId: Id, timestamp: Long)
//  case class UpdateLiveliness(timestamp: Long)
//}
//
//// Managing enqueued checks
//class ClusterManager extends Actor {
//  val HEARTBEAT_FREQUENCY = 1.minutes
//
//  var checkerCluster: Map[Id, AHost] = Map.empty
//  var checkinTimes: Map[Id, Long] = Map.empty
//
//  def receive = {
//    case ClusterJoin(host, timestamp) => {
//      checkerCluster = checkerCluster + ((host.id, host))
//      checkinTimes = checkinTimes + ((host.id, timestamp))
//    }
//    case Heartbeat(hostId, timestamp) =>
//      checkinTimes = checkinTimes.updated(hostId, timestamp)
//    case UpdateLiveliness(timestamp) => {
//      val removedHosts = checkinTimes.keys
//        .filter { id => (timestamp - checkinTimes(id)) >= HEARTBEAT_FREQUENCY.toMillis * 3 }
//        .toSet
//
//      checkerCluster = checkerCluster.filterKeys { !removedHosts.contains(_) }
//      checkinTimes = checkinTimes.filterKeys { !removedHosts.contains(_) }
//    }
//  }
//}
//
//// This actor manages the list of tasks currently available on the cluster, adding new ones as well as removing ones not
//// available anymore
//object JobAvailabilityManager {
//  case class JobsAvailable(jobs: Seq[Job])
//  case class JobsUnavailable(jobs: Seq[Job])
//
//  // :TODO: Remove this once the tube actor is separated out.
//  case class AddJob(job: Job)
//  case class DeleteJob(id: Id)
//}
//class JobAvailabilityManager extends Actor with ActorLogging {
//  import scala.concurrent.ExecutionContext.Implicits.global
//
//  var jobAvailability: Map[String, Int] = Map.empty
//  var jobIds: Map[String, Id] = Map.empty
//
//  def jobQueue: ActorRef = ???
//
//  private def makeAvailable(job: Job): Future[(String, Id)] = {
//    implicit val timeout = Timeout(30.seconds)
//    val idFuture: Future[Id] = (jobQueue ? AddJob(job)).mapTo[Id]
//    idFuture.map { (job.uniqId, _) }
//  }
//
//  private def makeUnavailable(job: Job): Unit = {
//    val id = jobIds(job.uniqId)
//    jobQueue ! DeleteJob(id)
//  }
//
//  // Either all jobs get updated or none!
//  def updateAvailability(jobs: Seq[Job], delta: Int): Unit  = {
//    var availMap: Map[String, Int] = jobAvailability
//
//    val futures = jobs.map { job =>
//      val availability = jobAvailability.getOrElse(job.uniqId, 0) + delta
//      require(availability >= 0)
//      availMap = availMap.updated(job.uniqId, availability)
//
//      if (delta > 0 && availability == 1)
//        makeAvailable(job).map(Some(_))
//      else Future {
//        if (availability == 0)
//          makeUnavailable(job)
//        None
//      }
//    }
//
//    val combinedFuture = Future.sequence(futures)
//    combinedFuture.onComplete {
//      case Success(options) => {
//        val newJobIds: Seq[(String, Id)] = options.flatten
//        jobIds = jobIds ++ newJobIds
//        jobAvailability = availMap
//        log.info(s"Updated ${jobs.length} jobs, ${newJobIds.length} new jobs available.")
//      }
//      case Failure(err) => {
//        log.error(s"Failed to make new jobs available! jobs=${jobs}", err)
//      }
//    }
//
//    Await.result(combinedFuture, 30.seconds)
//  }
//
//  def receive = {
//    case JobsAvailable(jobs: Seq[Job])   => updateAvailability(jobs, +1)
//    case JobsUnavailable(jobs: Seq[Job]) => updateAvailability(jobs, -1)
//  }
//}
