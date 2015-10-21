package com.github.macobo.checker

import akka.actor.{Cancellable, ActorSystem, Props}
import com.github.macobo.checker.server._
import scala.concurrent.duration._

object Server extends App {
  import scala.concurrent.ExecutionContext.Implicits.global
  import QueueCommunicator._
  val system = ActorSystem("checker-server")

  val queues = List(CLUSTER_QUEUE, RESULTS_QUEUE)

  val jobManager =
    system.actorOf(Props(new JobAvailabilityManager()), "job_availability_manager")
  val queueCommunicator =
    system.actorOf(Props(new QueueCommunicator(ServerMode, queues)), "queue")
  val clusterManager =
    system.actorOf(Props(new ClusterManager(jobManager)), "job_manager")
  val resultManager =
    system.actorOf(Props(new ResultManager()), "result_manager")
  val forwarder =
    system.actorOf(Props(new JobForwarder(resultManager, clusterManager)), "forwarder")

  // Check queue for new messages every few seconds
  val repeatedCheck: Cancellable = system.scheduler.schedule(
    0.seconds,
    5.seconds,
    queueCommunicator,
    CheckQueue(forwarder)
  )
  system.registerOnTermination { repeatedCheck.cancel() }
}
