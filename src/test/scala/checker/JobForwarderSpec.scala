package checker

import akka.actor.{Actor, ActorSystem}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import macobo.disque.commands.{Job, JobId}
import org.scalatest.{MustMatchers, WordSpecLike}

class MyActor extends Actor {
  def receive = {
    case _: QueueMessage => {}
  }
}

class JobForwarderSpec
  extends TestKit(ActorSystem())
  with ImplicitSender
  with WordSpecLike
  with MustMatchers
{
  def parse(string: String) =
    JobParser.parseMessage(string)

  "JobForwarder" should {
    val heartbeat = Heartbeat("foo.bar.zoo")
    val heartbeatMsg = """{"hostId":"foo.bar.zoo","messageType":"HEARTBEAT"}"""

    "input message" should {
      "deserializing" should {
        "for HeartBeat" in {
          parse(heartbeatMsg) must equal(heartbeat)
        }
      }
      "serializing" should {
        "for HeartBeat" in {
          JobParser.jsonify(heartbeat) must equal(heartbeatMsg)
        }
      }
    }

    "message forwarding" should {
      val resultManagerSibling = TestProbe()
      val clusterManagerSibling = TestProbe()
      val forwarder = TestActorRef(new JobForwarder(resultManagerSibling.ref, clusterManagerSibling.ref))

      "forwarding a heartbeat" in {
        forwarder ! Job(heartbeatMsg, JobId(""), None)
        clusterManagerSibling.expectMsg(heartbeat)
      }
    }
  }
}
