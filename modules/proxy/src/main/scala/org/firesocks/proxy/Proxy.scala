package org.firesocks.proxy

import akka.actor._
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import org.firesocks.app.Config
import org.firesocks.lang._
import org.firesocks.util.Logger

class Proxy(config: Config) extends Actor with Logger {
  import context.system

  require(config.mode == 'proxy)

  IO(Tcp) ! Bind(self, config.bind)

  override def receive: Receive = {
    case Bound(bnd) =>
      log.info("Serving at {}", bnd)
      context.become(bound)
    case CommandFailed(b: Bind) =>
      log.error("Failed to bind to {}", config.bind)
      context stop self
  }

  private def bound: Receive = {
    case Connected(client, _) =>
      log.info("Client {} connected.", client)
      val broker = Broker.mkActor(sender(), config)
      sender() ! Register(broker, keepOpenOnPeerClosed = true)
  }
}

object Proxy {
  def mkActor(system: ActorSystem, config: Config): ActorRef = {
    val clazz = classOf[Proxy]
    val name = mkActorName(clazz, "@", config.bind)
    val p = Props.create(clazz, config)
    system.actorOf(p, name)
  }
}
