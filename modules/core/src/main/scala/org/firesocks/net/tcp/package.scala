package org.firesocks.net

import akka.io.Tcp

import scala.concurrent.duration._
import scala.language.postfixOps

package object tcp {

  implicit val TIMEOUT = 60 seconds

  object Ack extends Tcp.Event

}
