package org.firesocks.net

import akka.actor.Actor
import org.firesocks.util.Logger

trait RelayCloseGuard { this: Actor with Logger =>

  private var localClosed = false
  private var remoteClosed = false

  def anyClosing: Boolean = localClosed || remoteClosed

  def fullyClosed: Boolean = localClosed && remoteClosed

  def setLocalClosed(b: Boolean = true): Unit = {
    localClosed = b
  }

  def setRemoteClosed(b: Boolean = true): Unit = {
    remoteClosed = b
  }

  def clearFlags(): Unit = {
    localClosed = false
    remoteClosed = false
  }

  def stopIfFullyClosedElse(code: => Unit): Unit = {
    if(fullyClosed) {
      log.info("Stopping {} on fully close.", self)
      context stop self
    } else code
  }
}
