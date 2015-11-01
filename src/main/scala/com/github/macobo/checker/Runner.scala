package com.github.macobo.checker

import java.net.InetAddress

import akka.actor.{ActorRef, ActorSystem, Cancellable, Props}
import com.github.macobo.checker.runner._
import com.github.macobo.checker.server._
import com.github.macobo.checker.server.protocol.Heartbeat

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Random

class Runner(collectors: List[CheckCollector])(implicit ec: ExecutionContext) extends App with CheckUtil {
  lazy val checkMap: Map[CheckId, CheckDefinition] = {
    val (checks, collisions) = merge(collectors.map { _.checks })
    require(collisions.isEmpty, s"Multiple definitions for checks: ${collisions.map { _._1 }}")
    checks
  }
  lazy val listings = checkMap.values.map { _.listing }.toList
  lazy val projectNames = projects(checkMap.keys)

  val system = ActorSystem("checker-runner")

  // TODO: Use hashids or something similar
  lazy val hostId =
    s"${InetAddress.getLocalHost.getHostName}--${Random.nextString(5)}"

  val queues = List(
    // messages to this particular runner
    QueueCommunicator.hostQueue(hostId)
  ) ++ projectNames.map { QueueCommunicator.projectQueue(_) }

  val queueCommunicator =
    system.actorOf(Props(new QueueCommunicator(RunnerMode, queues)), "queue")

  val notifier =
    system.actorOf(Props(new Notifier(hostId)), "notifier")

  // Let the server know we know how to run these checks.
  notifier ! Announce(listings)

  def createRepeatedMessage(actor: ActorRef, every: FiniteDuration, message: Any) = {
    val s: Cancellable = system.scheduler.schedule(
      0.seconds, every, actor, message
    )
    system.registerOnTermination { s.cancel() }
  }

  createRepeatedMessage(queueCommunicator, 5.seconds, CheckQueue(queueCommunicator))
  createRepeatedMessage(notifier, 5.seconds, Heartbeat)
}
