package checker

import akka.pattern.ask
import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestActorRef, TestProbe, ImplicitSender, TestKit}
import akka.util.Timeout
import org.scalatest.{MustMatchers, WordSpecLike}

import scala.util.Success
import scala.concurrent.duration._

class ClusterManagerSpec
  extends TestKit(ActorSystem())
  with ImplicitSender
  with WordSpecLike
  with MustMatchers
{
  def newManager = {
    val jobManager = TestProbe()
    (TestActorRef(new ClusterManager(jobManager.ref)), jobManager)
  }
  implicit val timeout = Timeout(2.seconds)

  def getState(manager: ActorRef) = {
    val Success(state) = (manager ? GetClusterState()).mapTo[ClusterState].value.get
    state
  }

  def join(host: Host, t: Long) =
    ClusterJoin(host.id, host.knownChecks, Some(t))

  val h1 = Host("h1", Seq(CheckListing(Check("tests", "h1"), 3.minutes, 2.minutes)))
  val h2 = Host("h2", Seq(CheckListing(Check("tests", "h1"), 3.minutes, 2.minutes)))
  val h3 = Host("h3", Seq(CheckListing(Check("tests", "h3"), 3.minutes, 2.minutes)))

  "Cluster manager" should {
    "allow hosts to join the cluster" in {
      val (manager, _) = newManager
      manager ! join(h1, 0)
      manager ! join(h2, 50)
      manager ! join(h3, 160)

      val hosts = getState(manager).hosts.sortBy { _._2 }
      hosts must equal(List((h1, 0), (h2, 50), (h3, 160)))
    }
  }
}
