package checker

import akka.actor.{ActorLogging, Actor}
import scala.concurrent.duration._

case class UpdateCluster(t: Option[Long]) extends Timestamped

class ClusterManager extends Actor with ActorLogging {
  val HEARTBEAT_FREQUENCY = 1.minutes

  var onlineHosts: Map[String, Host] = Map.empty
  var lastSeen: Map[String, Long] = Map.empty

  def isAliveAt(id: String, time: Long) =
    (time - lastSeen.getOrElse(id, 0L)) <= 2 * HEARTBEAT_FREQUENCY.toMillis

  def receive = {
    case m: ClusterJoin => {
      // :TODO: forward new jobs to JobManager
      onlineHosts = onlineHosts.updated(m.hostId, m.host)
      lastSeen = lastSeen.updated(m.hostId, m.timestamp)
    }
    case m: Heartbeat => {
      // :TODO: logic for checking a resurrection
      lastSeen = lastSeen.updated(m.hostId, m.timestamp)
    }
    case m: UpdateCluster => {
      // :TODO: let JobManager know jobs are unavailable
      val removedHosts = onlineHosts.keys
        .filterNot { isAliveAt(_, m.timestamp) }
        .toSet

      def filterFn(key: String): Boolean = !removedHosts.contains(key)

      onlineHosts = onlineHosts.filterKeys(filterFn)
      lastSeen = lastSeen.filterKeys(filterFn)
    }
  }
}
