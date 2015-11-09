package org.firesocks.net

import akka.actor.ActorRef

case class Forwarded(message: Any, via: ActorRef)
