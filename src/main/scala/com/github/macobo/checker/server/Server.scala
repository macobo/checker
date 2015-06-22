package com.github.macobo.checker

import akka.actor.{Props, ActorSystem}

class Server extends App {
  val system = ActorSystem("checker-server")

//  val jobForwarder = system.actorOf(Props(new ))
//  val queueCommunicator = system.actorOf(QueueCommunicator.props(), "queue")
}
