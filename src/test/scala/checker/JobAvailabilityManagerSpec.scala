package checker

import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import akka.util.Timeout
import checker.JobAvailabilityManager._
import macobo.disque.commands.JobId
import org.scalatest.{MustMatchers, WordSpecLike}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.reflect.ClassTag

class FakeQueue extends Actor {
  var calls: List[Any] = Nil

  def receive = {
    case call@MakeAvailable(listing) => {
      sender() ! JobId(listing.check.identifier)
      calls = calls ::: List(call)
    }
    case call: DeleteCheck =>
      calls = calls ::: List(call)
    case GetState() => sender() ! calls
  }
}

class JobAvailabilityManagerSpec
  extends TestKit(ActorSystem())
  with ImplicitSender
  with WordSpecLike
  with MustMatchers
{
  implicit val timeout = Timeout(2.seconds)
  def actors = {
    val queue = TestActorRef[FakeQueue]
    (TestActorRef(new JobAvailabilityManager(queue)), queue)
  }

  def getState[T:ClassTag](ref: ActorRef) =
    (ref ? GetState()).mapTo[T].value.get.get

  val c1 = CheckListing(Check("tests", "t1"), 3.minutes, 2.minutes)
  val c2 = CheckListing(Check("tests", "t2"), 3.minutes, 2.minutes)
  val c3 = CheckListing(Check("tests", "t3"), 3.minutes, 2.minutes)

  type CheckCount = Set[(Check, Int)]
  def expectJob(probe: TestProbe, job: CheckListing) = {
    probe.expectMsg(MakeAvailable(job))
    probe.reply(JobId(job.check.identifier))
  }

  "Cluster manager" should {
    "keep correct availability counts when checks are added" in {
      val (actor, _) = actors
      actor ! JobsAvailable(List(c1, c2))
      actor ! JobsAvailable(List(c1))
      actor ! JobsAvailable(List(c1, c3))

      getState[CheckCount](actor) must equal(Set((c1.check, 3), (c2.check, 1), (c3.check, 1)))
    }

    "keep correct availability counts when checks are deleted" in {
      val (actor, _) = actors
      actor ! JobsAvailable(List(c1, c2, c3))
      actor ! JobsAvailable(List(c1))

      actor ! JobsUnavailable(List(c1, c2))

      getState[CheckCount](actor) must equal(Set((c1.check, 1), (c3.check, 1)))
    }

    "add multi-available checks only once to queue" in {
      val (actor, queue) = actors
      actor ! JobsAvailable(List(c1, c2))
      actor ! JobsAvailable(List(c1, c3))

      val expected = List(MakeAvailable(c1), MakeAvailable(c2), MakeAvailable(c3))
      getState[List[Any]](queue) must equal(expected)
    }

    "re-add checks which are made unavailable" in {
      val (actor, queue) = actors
      actor ! JobsAvailable(List(c1, c2))
      actor ! JobsAvailable(List(c1))
      actor ! JobsUnavailable(List(c1, c2))
      actor ! JobsAvailable(List(c1, c2))

      val expected = List(
        MakeAvailable(c1), MakeAvailable(c2),
        DeleteCheck(JobId(c2.check.identifier)),
        MakeAvailable(c2))
      getState[List[Any]](queue) must equal(expected)
    }
  }
}
