package checker

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
//import main.scala.checker.JobAvailabilityManager
import org.scalatest.WordSpecLike

class JobAvailabilityManagerSpec
  extends TestKit(ActorSystem())
  with ImplicitSender
  with WordSpecLike
{
//  trait Context {
//    val jobManager: ActorRef = system.actorOf(Props(new JobAvailabilityManager))
//  }
//
//  "JobAvailabilityManager" when {
//    "empty" should {
//
//    }
//  }
}
