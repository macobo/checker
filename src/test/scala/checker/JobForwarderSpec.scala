package checker

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import macobo.disque.commands.{Job, JobId}
import org.scalatest.{MustMatchers, WordSpecLike}

class JobForwarderSpec
  extends TestKit(ActorSystem())
  with ImplicitSender
  with WordSpecLike
  with MustMatchers
{
  def parse = JobParser.parseMessage _
  def jsonify = JobParser.jsonify _
  def job(x: String) = Job(x, JobId(""), None)

  "JobForwarder" should {
    val heartbeat = Heartbeat("foo.bar.zoo")
    val heartbeatMsg = """{"hostId":"foo.bar.zoo","messageType":"HEARTBEAT"}"""

    val checkresult = CheckResultMessage(Check("project", "checkname"), CheckSuccess(), "loglog", 1000)
    val checkresultMsg = """{"check":{"project":"project","name":"checkname"},"result":{"success":true},"log":"loglog","timeTaken":1000,"messageType":"CHECKRESULT"}"""

    "input message" should {
      "deserializing" should {
        "for HeartBeat" in {
          parse(heartbeatMsg) must equal(heartbeat)
        }

        "for CheckResultMessage" in {
          parse(checkresultMsg) must equal(checkresult)
        }
      }
      "serializing" should {
        "for HeartBeat" in {
          jsonify(heartbeat) must equal(heartbeatMsg)
        }

        "for CheckResultMessage" in {
          jsonify(checkresult) must equal(checkresultMsg)
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
