package com.github.macobo.checker.runner

import com.github.macobo.checker.server.{CheckListing, CheckId}

import scala.concurrent.duration._

class Lazy[T](wrp: => T) {
  lazy val value: T = wrp
}

// A concrete check that can be run.
case class CheckDefinition(
  check: CheckId,
  registerFn: (CheckDefinition => Unit),
  testFunction: Option[Lazy[Unit]] = None,
  runsEvery: Duration = 5.seconds,
  timeout: Duration = 3.seconds
  ) {
  def runsEvery(duration: => Duration): CheckDefinition =
    copy(runsEvery = duration)

  def timeout(duration: Duration): CheckDefinition =
    copy(timeout = duration)

  def check(blk: => Unit): CheckDefinition = {
    val definition = copy(testFunction = Some(new Lazy(blk)))
    registerFn(definition)
    definition
  }

  def listing: CheckListing =
    CheckListing(check, runsEvery, timeout)

  def apply(blk: => Unit) = check(blk)
}

trait CheckCollector {
  var checks: Map[CheckId, CheckDefinition] = Map.empty

  protected def register(definition: CheckDefinition): Unit = {
    require(!checks.contains(definition.check),
      s"Multiple checks with same name in ${definition.check.project}: ${definition.check.name}")

    checks = checks.updated(definition.check, definition)
  }
}

trait CheckProject extends CheckCollector {
  import scala.language.implicitConversions

  implicit def stringToDef(name: String) =
    CheckDefinition(
      CheckId(projectName, name),
      register,
      testFunction = None,
      runsEvery = defaultRunFrequency,
      timeout = defaultTimeout
    )

  /** Name of the project under test */
  val projectName: String

  /** Overridable defaults for tests */
  val defaultRunFrequency: Duration = 1.hour
  val defaultTimeout: Duration = 10.minutes

}
