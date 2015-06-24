package com.github.macobo.checker.helpers

import com.github.macobo.checker.server.{QueueMessage, Serializer}
import macobo.disque.DisqueClient
import spray.json._

trait DisqueQueue {
  lazy private val client = new DisqueClient("localhost", 7711)

  def readJob[T <: QueueMessage](queue: String)(implicit  evidence$1: JsonReader[T]): Option[T] = {
    client.getJob(queue, Some(10)).map { job =>
      job.value.parseJson.convertTo[T]
    }
  }
}
