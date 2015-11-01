package com.github.macobo.checker.runner

import com.github.macobo.checker.server.{CheckId, CheckListing}

/** Util functions for manipulating collections of checks */
trait CheckUtil {
  // Merge two maps and return result, collisions
  def merge[K, V](maps: Seq[Map[K, V]]): (Map[K, V], List[(K, V)]) =
    maps.foldLeft((Map.empty[K, V], List.empty[(K, V)])) { case (result, thisMap) =>
      thisMap.foldLeft(result) { case ((rMap, collisions), (key, v)) =>
        rMap.get(key) match {
          case Some(existing) => (rMap, (key, v) :: collisions)
          case None => (rMap.updated(key, v), collisions)
        }
      }
    }

  def projects(checks: Iterable[CheckId]): Set[String] =
    checks.map { _.project }.toSet
}
