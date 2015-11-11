package org.firesocks.util

import akka.actor.Actor
import akka.event.{LogSource, Logging}
import akka.event.LogSource._

trait Logger {
  self: Actor =>

  lazy val log = Logging(
    context.system.eventStream,
    s"${getClass.getSimpleName}-${this.hashCode}"
  )
}
