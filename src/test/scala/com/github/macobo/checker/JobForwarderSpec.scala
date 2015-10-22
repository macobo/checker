package com.github.macobo.checker

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import com.github.macobo.checker.server._
import org.scalatest.{MustMatchers, WordSpecLike}
import scala.concurrent.duration._
import Serializer._
import spray.json._

class JobForwarderSpec
  extends TestKit(ActorSystem())
  with ImplicitSender
  with WordSpecLike
  with MustMatchers
{
  def job(x: String) = Job(x, "", None)
  def parse(x: String) = x.parseJson.convertTo[QueueMessage]

  "JobForwarder" when {
    val heartbeat = Heartbeat("foo.bar.zoo")
    val heartbeatMsg = """{"host_id":"foo.bar.zoo","message_type":"HEARTBEAT"}"""

    val checkresult = CheckResultMessage(Check("project", "checkname"), CheckSuccess(), "loglog", 1.second)
    val checkresultMsg =
      """
        |{
        |   "check": {
        |     "project": "project",
        |     "name": "checkname"
        |   },
        |   "result": {"success": true},
        |   "log":"loglog",
        |   "time_taken":1000,
        |   "message_type":"CHECKRESULT"
        | }""".stripMargin

    val clusterJoin = ClusterJoin("foo.bar", List(CheckListing(Check("project", "checkname"), 5.seconds, 2.seconds)))
    val clusterJoinMsg =
      """
        | {
        |   "host_id": "foo.bar",
        |   "known_checks": [
        |     {
        |       "check": {
        |         "project": "project",
        |         "name": "checkname"
        |       },
        |       "runs_every": 5000,
        |       "timelimit": 2000
        |     }
        |   ],
        |   "message_type": "CLUSTER_JOIN"
        | }
      """.stripMargin

    "input message" when {
      "deserializing" should {
        "for HeartBeat" in {
          parse(heartbeatMsg) must equal(heartbeat)
        }

        "for CheckResultMessage" in {
          parse(checkresultMsg) must equal(checkresult)
        }

        "for ClusterJoin" in {
          parse(clusterJoinMsg) must equal(clusterJoin)
        }
      }
      "serializing" should {
        "for HeartBeat" in {
          heartbeat.toJson must equal(heartbeatMsg.parseJson)
        }

        "for CheckResultMessage" in {
          checkresult.toJson must equal(checkresultMsg.parseJson)
        }

        "for ClusterJoin" in {
          clusterJoin.toJson must equal(clusterJoinMsg.parseJson)
        }
      }
    }

    "message forwarding" should {
      val resultman = TestProbe()
      val cluster = TestProbe()
      val forwarder = TestActorRef(new JobForwarder(resultman.ref, cluster.ref))

      "forwarding a heartbeat" in {
        forwarder ! job(heartbeatMsg)
        cluster.expectMsg(heartbeat)
      }

      "forwarding a checkresult" in {
        forwarder ! job(checkresultMsg)
        resultman.expectMsg(checkresult)
      }
    }
  }
}
