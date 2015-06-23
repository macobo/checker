package com.github.macobo.checker.runner

import java.net.InetAddress

import akka.actor.{ActorLogging, Actor}
import com.github.macobo.checker.server.{Enqueue, ClusterJoin, Heartbeat, CheckListing}

import scala.util.Random

class Notifier(hostId: String) extends Actor with ActorLogging {
  import com.github.macobo.checker.server.QueueCommunicator._
  def queue = context.actorSelection("../queue")

  def receive = {
    case checks: List[CheckListing] => {
      self ! ClusterJoin
      context.become(notifier(checks))
    }
  }

  def notifier(checks: List[CheckListing]): Receive = {
    case ClusterJoin => {
      log.info(s"Notifying that we have joined the cluster. hostId=${hostId}, checks=${checks}")
      queue ! Enqueue(CLUSTER_QUEUE, ClusterJoin(hostId, checks))
    }
    case Heartbeat =>
      queue ! Enqueue(CLUSTER_QUEUE, Heartbeat(hostId))
  }
}
