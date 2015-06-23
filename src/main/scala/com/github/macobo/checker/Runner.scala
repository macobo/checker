package com.github.macobo.checker

import java.net.InetAddress

import akka.actor.{Cancellable, ActorRef, Props, ActorSystem}
import com.github.macobo.checker.runner.Notifier
import com.github.macobo.checker.server._

import scala.concurrent.duration._
import scala.util.Random

object Runner extends App {
  import scala.concurrent.ExecutionContext.Implicits.global
  val system = ActorSystem("checker-runner")

  // TODO: Use hashids or something similar
  lazy val hostId =
    s"${InetAddress.getLocalHost.getHostName}--${Random.nextString(5)}"

  val queues = List(
    // messages to this particular runner
    s"checker::runner::${hostId}"
  )
  val checks = List(
    CheckListing(Check("foo", "bar"), 20.seconds, 5.seconds),
    CheckListing(Check("foo", "zoo"), 7.seconds, 5.seconds)
  )

  val queueCommunicator =
    system.actorOf(Props(new QueueCommunicator("runner", queues)), "queue")

  val notifier =
    system.actorOf(Props(new Notifier(hostId)), "notifier")

  notifier ! checks

  def createRepeatedMessage(actor: ActorRef, every: FiniteDuration, message: Any) = {
    val s: Cancellable = system.scheduler.schedule(
      0.seconds, every, actor, message
    )
    system.registerOnTermination { s.cancel() }
  }

  createRepeatedMessage(queueCommunicator, 5.seconds, CheckQueue(queueCommunicator))
  createRepeatedMessage(notifier, 5.seconds, Heartbeat)
}
