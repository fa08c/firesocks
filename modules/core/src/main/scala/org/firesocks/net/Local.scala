package org.firesocks.net

import akka.actor.ActorRef

case class Local(conn: ActorRef, forwarding: Boolean = false)
