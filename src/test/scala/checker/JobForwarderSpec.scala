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
  implicit val formats = DefaultFormats

  def parse(string: String) =
    JobParser.parseMessage(string)

  "JobForwarder" should {
    val forwarder = TestActorRef[JobForwarder]
    val clusterManagerSibling = TestActorRef(Props(new MyActor()), "cluster_manager")
    val resultManagerSibling = TestActorRef(Props(new MyActor()), "result_manager")

    "input message" should {
      "deserializing" should {
        "for HeartBeat" in {
          val message =
            """
              |{
              |  "messageType": "HEARTBEAT",
              |  "hostId": "foo.bar.zoo"
              |}
            """.stripMargin
          parse(message) must equal(Heartbeat("foo.bar.zoo"))
        }
      }
    }
  }
}
