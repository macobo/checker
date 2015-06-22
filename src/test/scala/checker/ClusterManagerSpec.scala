package checker

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import akka.util.Timeout
import checker.JobAvailabilityManager.{JobsUnavailable, JobsAvailable}
import org.scalatest.{MustMatchers, WordSpecLike}

import scala.concurrent.duration._
import scala.util.Success

class ClusterManagerSpec
  extends TestKit(ActorSystem())
  with ImplicitSender
  with WordSpecLike
  with MustMatchers
{
  implicit val timeout = Timeout(2.seconds)
  def actors = {
    val jobManager = TestProbe()
    (TestActorRef(new ClusterManager(jobManager.ref)), jobManager)
  }

  def getState(manager: ActorRef) = {
    val Success(state) = (manager ? GetClusterState()).mapTo[ClusterState].value.get
    state
  }

  def getHosts(manager: ActorRef) =
    getState(manager).hosts.sortBy { _._2 }

  def join(host: Host, t: Long) =
    ClusterJoin(host.id, host.knownChecks, Some(t))

  val c1 = CheckListing(Check("tests", "t1"), 3.minutes, 2.minutes)
  val c2 = CheckListing(Check("tests", "t2"), 3.minutes, 2.minutes)
  val c3 = CheckListing(Check("tests", "t3"), 3.minutes, 2.minutes)
  val h1 = Host("h1", Seq(c1, c2))
  val h2 = Host("h2", Seq(c3))
  val h3 = Host("h3", Seq(c1))

  "Cluster manager" should {
    "allow hosts to join the cluster" in {
      val (manager, _) = actors
      manager ! join(h1, 0)
      manager ! join(h2, 50000)
      manager ! join(h3, 160000)

      getHosts(manager) must equal(List((h1, 0), (h2, 50000), (h3, 160000)))
    }

    "update hosts when they have timed out" in {
      val (manager, _) = actors
      manager ! join(h1, 0)
      manager ! join(h2, 50000)
      manager ! join(h3, 160000)

      manager ! UpdateCluster(Some(170000))
      getHosts(manager) must equal(List((h2, 50000), (h3, 160000)))
    }

    "let jobmanager know of new jobs that are available" in {
      val (manager, jobs) = actors
      manager ! join(h1, 0)
      manager ! join(h2, 50000)
      manager ! join(h3, 160000)

      jobs.expectMsg(JobsAvailable(Seq(c1, c2)))
      jobs.expectMsg(JobsAvailable(Seq(c3)))
      jobs.expectMsg(JobsAvailable(Seq(c1)))
    }

    "let jobmanager know of jobs that are unavailable" in {
      val (manager, jobs) = actors
      manager ! join(h1, 0)
      manager ! join(h2, 50000)
      manager ! join(h3, 160000)

      // Found no good way of popping these messages. :(
      jobs.expectMsg(JobsAvailable(Seq(c1, c2)))
      jobs.expectMsg(JobsAvailable(Seq(c3)))
      jobs.expectMsg(JobsAvailable(Seq(c1)))

      manager ! UpdateCluster(Some(170000))
      jobs.expectMsg(JobsUnavailable(Seq(c1, c2)))
    }

    "let jobmanager update using heartbeats" in {
      val (manager, _) = actors
      manager ! join(h1, 0)
      manager ! join(h2, 50000)
      manager ! Heartbeat(h1.id, Some(60000))

      getHosts(manager) must equal(List((h2, 50000), (h1, 60000)))
    }
  }
}
