package com.github.macobo.checker.helpers

import biz.paluch.spinach.{DisqueClient, DisqueURI}
import com.github.macobo.checker.server.protocol.QueueMessage

import scala.concurrent.duration._
import spray.json._

trait DisqueQueue {
  lazy val commands = {
    val client = new DisqueClient(DisqueURI.create("localhost", 7711))
    client.connect().sync()
  }

  def readJob[T <: QueueMessage](queue: String)(implicit  evidence$1: JsonReader[T]): Option[T] = {
    commands.getjob(10, SECONDS, queue) match {
      case null => None
      case x => Some(x.getBody.parseJson.convertTo[T])
    }
  }
}
