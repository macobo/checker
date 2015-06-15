package checker

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.json4s.DefaultFormats
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
    val forwarder = TestActorRef[JobForwarder]
    val clusterManagerSibling = TestActorRef(Props(new MyActor()), "cluster_manager")
    val resultManagerSibling = TestActorRef(Props(new MyActor()), "result_manager")

    "input message" should {
      val heartbeat = Heartbeat("foo.bar.zoo")
      val heartbeatMsg = """{"hostId":"foo.bar.zoo","messageType":"HEARTBEAT"}"""

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
  }
}
