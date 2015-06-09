package main.scala.checker

import scala.concurrent.duration.Duration

// Projects and checks
case class Project(name: String)
case class ACheck(project: Project, name: String) // :TODO: Name validation, frequency

// Check results
trait CheckLog
case class RunInfo(startTime: Long, duration: Duration)

sealed trait CheckResult {
  def check: ACheck
  def log: CheckLog
  def runInfo: RunInfo
}
case class SuccessResult(check: ACheck, log: CheckLog, runInfo: RunInfo) extends CheckResult
case class FailResult(check: ACheck, description: String, log: CheckLog, runInfo: RunInfo) extends CheckResult

// History entries
sealed trait HistoryEntry {
  def check: ACheck
  def startTime: Long // :TODO:
  def duration: Long
  def status: String // :TODO: Enum-ify?
}

case class MinimalHistoryEntry(
  check: ACheck,
  status: String,
  startTime: Long,
  duration: Long
) extends HistoryEntry

case class DetailedHistoryEntry(result: CheckResult) extends HistoryEntry {
  def check: ACheck = result.check
  def log: CheckLog = result.log
  def startTime: Long = result.runInfo.startTime
  def duration: Long = result.runInfo.duration.toMillis
  def status = result match {
    case SuccessResult(_, _) => "success"
    case FailResult(_, _) => "fail"
  }
}

// History logs
case class ProjectHistory[E <: HistoryEntry](history: Map[ACheck, Seq[E]], maxSize: Option[Int] = Nil)
case class AbsoluteHistory[E <: HistoryEntry](project: Map[Project, ProjectHistory[E]], maxSize: Option[Int] = Nil)

// Rest of the registry state - known hosts
case class Id(value: String)
case class AHost(id: Id, knownChecks: Set[ACheck]) {
  def projects: Set[Project] = ???
  def checksForProject(project: Project): Set[ACheck] = ???
}

case class CheckRegistry(registry: Map[Project, ACheck])
sealed case class HostRegistry(hosts: Map[Id, AHost]) {
  def checkRegistry: CheckRegistry = ???
}

// We consider hosts alive if they check in periodically
case class AliveHostRegistry(hosts: Map[Id, AHost], lastHeartBeat: Map[Id, Long]) {
  def update(t: Long): AliveHostRegistry = ???
  def checkin(id: Id): Unit = ???
}

case class SchedulerState(hosts: AliveHostRegistry, history: AbsoluteHistory[DetailedHistoryEntry])
