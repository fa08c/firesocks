package org.firesocks.net.tcp

import java.net.InetSocketAddress

import org.firesocks.codec.{Encoded, Plain, Codec}
import org.firesocks.net.Forwarded
import org.firesocks.util.Logger

import akka.actor.{ActorContext, Actor, ActorRef, Props}
import akka.io.{IO,Tcp}

import org.firesocks.lang._

class TCPRelay(instigator: ActorRef,
               remoteAddr: InetSocketAddress,
               codec: Codec) extends Actor with Logger {
  import context.system
  import Tcp._

  IO(Tcp) ! Connect(remoteAddr)

  override def receive: Receive = {
    case Connected(peer, _) =>
      log.info("Connecting to {}", peer)

      val remote = sender()
      remote ! Register(self)
      instigator ! Register(self)

      context become connected(remote)

    case CommandFailed(c: Connect) =>
      log.error("Failed to connect to {}", c.remoteAddress)
      context stop self
  }

  private def connected(remote: ActorRef): Receive = {
    case Forwarded(Received(bytes), via) if via == instigator =>
      remote ?! Write(codec.encode(Plain(bytes)).bytes, Ack)

    case Received(bytes) if sender() == instigator =>
      remote ?! Write(codec.encode(Plain(bytes)).bytes, Ack)

    case Received(bytes) if sender() == remote =>
      instigator ?! Write(codec.decode(Encoded(bytes)).bytes, Ack)

    case PeerClosed if sender() == instigator =>
      log.info("Instigator peer closed.")
      context stop self

    case PeerClosed if sender() == remote =>
      log.info("Remote peer closed.")
      context stop self
  }
}

object TCPRelay {
  def mkActor(instigator: ActorRef, remoteAddr: InetSocketAddress, codec: Codec)
             (implicit context: ActorContext): ActorRef = {
    val clazz = classOf[TCPRelay]
    val name = mkActorName(clazz, ":", instigator, "~", remoteAddr)
    val p = Props.create(clazz, instigator, remoteAddr, codec)
    context.actorOf(p, name)
  }
}
