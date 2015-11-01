package com.github.macobo.checker.server.protocol

import com.github.macobo.checker.server._
import spray.json._

import scala.concurrent.duration._

object Serializer extends DefaultJsonProtocol {
  implicit object DurationFormat extends RootJsonFormat[Duration] {
    def write(x: Duration) =
      JsNumber(x.toMillis)
    def read(x: JsValue) = x match {
      case JsNumber(ms) => ms.toInt.millis
      case _ => deserializationError("Number of milliseconds expected")
    }
  }

  implicit val checkFormat = jsonFormat2(CheckId)
  implicit val checkListingFormat = jsonFormat(CheckListing, "check", "runs_every", "timelimit")

  implicit object CheckResultTypeFormat extends RootJsonFormat[CheckResultType] {
    def write(x: CheckResultType) = x match {
      case CheckSuccess() => JsObject("success" -> JsBoolean(true))
      case CheckFailure(reason) => JsObject("success" -> JsBoolean(false), "reason" -> JsString(reason))
    }
    def read(x: JsValue) = x.asJsObject.getFields("success") match {
      case Seq(JsBoolean(true)) => CheckSuccess()
      case Seq(JsBoolean(false), JsString(reason)) => CheckFailure(reason)
    }
  }

  implicit object HeartBeatFormat extends RootJsonFormat[Heartbeat] {
    def write(x: Heartbeat) = x match {
      case Heartbeat(hostId, _) =>
        JsObject(
          "message_type" -> JsString("HEARTBEAT"),
          "host_id" -> JsString(hostId)
        )
    }

    def read(value: JsValue) = value.asJsObject.getFields("host_id") match {
      case Seq(JsString(hostId)) =>
        Heartbeat(hostId)
    }
  }


  implicit object ClusterJoinFormat extends RootJsonFormat[RunnerJoin] {
    def write(x: RunnerJoin) = x match {
      case RunnerJoin(hostId, checks, _) =>
        JsObject(
          "message_type" -> JsString("RUNNER_JOIN"),
          "host_id" -> JsString(hostId),
          "known_checks" -> checks.toJson
        )
    }

    def read(value: JsValue) = value.asJsObject.getFields("host_id", "known_checks") match {
      case Seq(JsString(hostId), checks) =>
        RunnerJoin(hostId, checks.convertTo[Seq[CheckListing]])
    }
  }

  implicit object CheckResultFormat extends RootJsonFormat[CheckResultMessage] {
    def write(x: CheckResultMessage) = x match {
      case CheckResultMessage(check, result, log, timeTaken, _) =>
        JsObject(
          "message_type" -> JsString("CHECKRESULT"),
          "check" -> check.toJson,
          "result" -> result.toJson,
          "log" -> log.toJson,
          "time_taken" -> timeTaken.toJson
        )
    }

    def read(value: JsValue) = value.asJsObject.getFields("check", "result", "log", "time_taken") match {
      case Seq(check, result, JsString(log), timeTaken) =>
        CheckResultMessage(
          check.convertTo[CheckId],
          result.convertTo[CheckResultType],
          log,
          timeTaken.convertTo[Duration]
        )
    }
  }

  implicit object FormatQM extends RootJsonFormat[QueueMessage] {
    def write(m: QueueMessage) = m match {
      case x: Heartbeat => x.toJson
      case x: RunnerJoin => x.toJson
      case x: CheckResultMessage => x.toJson
    }

    def read(x: JsValue) = {
      x.asJsObject.fields("message_type") match {
        case JsString("HEARTBEAT") => x.convertTo[Heartbeat]
        case JsString("CLUSTER_JOIN") => x.convertTo[RunnerJoin]
        case JsString("CHECKRESULT") => x.convertTo[CheckResultMessage]
        case x => deserializationError(s"Expected message type, got ${x}")
      }
    }
  }
}
