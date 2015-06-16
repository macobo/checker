package checker

import akka.pattern.ask
import akka.actor.{Actor, ActorSystem}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import akka.util.Timeout
import checker.JobAvailabilityManager.{GetState, MakeAvailable, JobsAvailable}
import macobo.disque.commands.JobId
import org.scalatest.{MustMatchers, WordSpecLike}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

class FakeQueue extends Actor {
  var enqueued = Map.empty[CheckListing, Int]

  def receive = {
    case MakeAvailable(listing) => {
      sender() ! JobId(listing.check.identifier)
      enqueued = enqueued.updated(listing, enqueued.getOrElse(listing, 0)+1)
    }
    case GetState() => sender() ! enqueued
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

  val c1 = CheckListing(Check("tests", "t1"), 3.minutes, 2.minutes)
  val c2 = CheckListing(Check("tests", "t2"), 3.minutes, 2.minutes)
  val c3 = CheckListing(Check("tests", "t3"), 3.minutes, 2.minutes)

  def expectJob(probe: TestProbe, job: CheckListing) = {
    probe.expectMsg(MakeAvailable(job))
    probe.reply(JobId(job.check.identifier))
  }

  "Cluster manager" should {
    "keep correct availability counts" in {
      val (actor, _) = actors
      actor ! JobsAvailable(List(c1, c2))
      actor ! JobsAvailable(List(c1))
      actor ! JobsAvailable(List(c1, c3))

      val Success(state) = (actor ? GetState()).mapTo[Set[(CheckListing, Int)]].value.get
      state must equal(Set((c1.check, 3), (c2.check, 1), (c3.check, 1)))
    }
  }
}
