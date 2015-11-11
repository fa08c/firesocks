package org.firesocks.net.tcp

import java.net.InetSocketAddress

import org.firesocks.codec.{Encoded, Plain, Codec}
import org.firesocks.net.{Local, RelayCloseGuard}
import org.firesocks.util.Logger

import akka.actor.{ActorContext, Actor, ActorRef, Props}
import akka.io.{IO,Tcp}

import org.firesocks.lang._

class TCPRelay(local: Local, remoteAddr: InetSocketAddress, codec: Codec)
  extends Actor with Logger with RelayCloseGuard {
  import context.system
  import Tcp._

  IO(Tcp) ! Connect(remoteAddr)

  override def postStop(): Unit = {
    log.info("Relay closed.")
  }

  override def receive: Receive = {
    case Connected(peer, _) =>
      log.info("Connecting to {}", peer)

      val remote = sender()
      remote ! Register(self)

      if(local.forwarding) {
        context.parent ! Register(self)
      } else {
        local.conn ! Register(self, keepOpenOnPeerClosed = true)
      }

      context become connected(remote)

    case CommandFailed(c: Connect) =>
      log.error("Failed to connect to {}", c.remoteAddress)
      context stop self
  }

  private def connected(remote: ActorRef): Receive = {
    case Received(bytes) if sender() == local.conn =>
      remote ?! Write(codec.encode(Plain(bytes)).bytes, Ack)

    case Received(bytes) if sender() == remote =>
      local.conn ?! Write(codec.decode(Encoded(bytes)).bytes, Ack)

    case (PeerClosed | ConfirmedClosed) if sender() == local.conn =>
      log.info("Local peer closed.")
      setLocalClosed()
      stopIfFullyClosedElse {
        remote ?! ConfirmedClose
      }

    case (PeerClosed | ConfirmedClosed) if sender() == remote =>
      log.info("Remote peer closed.")
      setRemoteClosed()
      stopIfFullyClosedElse {
        local.conn ?! ConfirmedClose
      }
  }
}

object TCPRelay {
  def mkActor(local: Local, remoteAddr: InetSocketAddress, codec: Codec)
             (implicit context: ActorContext): ActorRef = {
    val clazz = classOf[TCPRelay]
    val name = mkActorName(clazz, ":", local.conn, "~", remoteAddr)
    val p = Props.create(clazz, local, remoteAddr, codec)
    context.watch(context.actorOf(p, name))
  }
}
