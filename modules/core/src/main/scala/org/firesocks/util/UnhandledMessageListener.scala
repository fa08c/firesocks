package org.firesocks.util

import akka.actor.{UnhandledMessage, Actor}

class UnhandledMessageListener extends Actor with Logger {
  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, classOf[UnhandledMessage])
  }

  override def postStop(): Unit = {
    context.system.eventStream.unsubscribe(self)
  }

  override def receive: Receive = {
    case UnhandledMessage(msg, sender, receiver) =>
      log.warning("Unhandled message {} from {} to {}", msg, sender, receiver)
  }
}
