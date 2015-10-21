package com.github.macobo.checker.runner

import akka.actor.{Actor, ActorLogging}
import com.github.macobo.checker.server._

case class Announce(checks: List[CheckListing])

class Notifier(hostId: String) extends Actor with ActorLogging {
  import com.github.macobo.checker.server.QueueCommunicator._
  def queue = context.actorSelection("../queue")

  def notifier(checks: List[CheckListing]): Receive = {
    case ClusterJoin => {
      log.info(s"Notifying that we have joined the cluster. hostId=${hostId}, checks=${checks}")
      queue ! Enqueue(QueueCommunicator.CLUSTER_QUEUE, ClusterJoin(hostId, checks))
    }
    case Heartbeat =>
      queue ! Enqueue(QueueCommunicator.CLUSTER_QUEUE, Heartbeat(hostId))
  }

  def receive = {
    case Announce(checks) => {
      self ! ClusterJoin
      context.become(notifier(checks.toList))
    }
  }
}
