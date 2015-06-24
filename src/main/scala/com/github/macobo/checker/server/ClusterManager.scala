package com.github.macobo.checker.server

import akka.actor.{Actor, ActorLogging, ActorRef}
import JobAvailabilityManager.{JobsAvailable, JobsUnavailable}
import com.github.macobo.checker.server.ClusterManager.{ClusterState, GetClusterState, UpdateCluster}
import org.joda.time.DateTime

import scala.concurrent.duration._

object ClusterManager {
  val HEARTBEAT_FREQUENCY = 1.minutes

  case class UpdateCluster(t: Option[Long]) extends Timestamped

  case class GetClusterState()

  case class ClusterState(hosts: List[(Host, Long)])
}

/* ClusterManager manages a fleet of check runners, checking their health and allowing new machines to join the fleet
 * dynamically.
 */
class ClusterManager(jobManager: ActorRef) extends Actor with ActorLogging {
  import ClusterManager._

  var onlineHosts: Map[String, Host] = Map.empty
  var lastSeen: Map[String, Long] = Map.empty

  def isAliveAt(id: String, time: Long) =
    (time - lastSeen.getOrElse(id, 0L)) <= 2 * HEARTBEAT_FREQUENCY.toMillis

  def updateCluster(timestamp: Long) = {
    val removedHosts = onlineHosts.keys
      .filterNot { isAliveAt(_, timestamp) }
      .toSet

    def filterFn(key: String): Boolean = !removedHosts.contains(key)

    val removedChecks: Seq[CheckListing] = removedHosts.toSeq.flatMap { onlineHosts(_).knownChecks }
    if (!removedChecks.isEmpty)
      jobManager ! JobsUnavailable(removedChecks)

    onlineHosts = onlineHosts.filterKeys(filterFn)
    lastSeen = lastSeen.filterKeys(filterFn)
  }

  def receive = {
    case m: ClusterJoin => {
      jobManager ! JobsAvailable(m.knownChecks)
      log.info(s"${m.hostId} joined the runner cluster! Projects: ${m.host.projects.mkString(", ")}")
      onlineHosts = onlineHosts.updated(m.hostId, m.host)
      lastSeen = lastSeen.updated(m.hostId, m.timestamp)
    }
    case m: Heartbeat => {
      // :TODO: if this is from an unseen host, send it a message to try to re-announce it (with a short TTL).
      require(lastSeen.contains(m.hostId))
      log.debug(s"Heartbeat from ${m.hostId} at ${new DateTime(m.timestamp)}")
      lastSeen = lastSeen.updated(m.hostId, m.timestamp)
    }
    case m: UpdateCluster => updateCluster(m.timestamp)
    case GetClusterState() => {
      val state = onlineHosts.keys.map { key =>
        (onlineHosts(key), lastSeen(key))
      }
      sender() ! ClusterState(state.toList)
    }
  }
}
