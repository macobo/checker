package com.github.macobo.checker

import com.github.macobo.checker.server.{CheckListing, CheckId}
import org.joda.time.DateTime
import org.scalatest.{MustMatchers, WordSpecLike}

import scala.concurrent.duration._

class CheckSpec extends WordSpecLike with MustMatchers {
  val check = CheckId("proj", "name")

  "CheckListing next run start time calculation" should {
    "give correct next run when starting from epoch" in {
      val listing = CheckListing(check, 3000.millis, 1.second)
      listing.nextRun(5000) must equal(6000)
      listing.nextRun(90000) must equal(93000)
    }

    "give correct next run time when starting from a random date" in {
      val date = new DateTime(2015, 6, 6, 0, 0)
      val ms = date.getMillis
      val listing = CheckListing(check, 320.seconds, 20.seconds)

      listing.nextRun(ms) must equal(ms + 320.seconds.toMillis)
      listing.nextRun(ms + 3.hours.toMillis) must equal(ms + (3.hours + 80.seconds).toMillis)
    }
  }
}
