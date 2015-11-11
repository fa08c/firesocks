package org.firesocks.net.ws.client

import java.net.URI

import akka.actor._
import akka.io.Tcp
import org.firesocks.codec.{Codec, Encoded, Plain}
import org.firesocks.lang._
import org.firesocks.net._
import org.firesocks.net.tcp.Ack

class WSRelay(local: Local, uri: URI, codec: Codec)
  extends WSClient(uri) with RelayCloseGuard {
  import Tcp._

  private implicit val TCP_TIMEOUT = tcp.TIMEOUT

  override def postStop(): Unit = {
    log.info("Relay closed.")
  }

  override def receive: Actor.Receive = {
    case WSOpen(handshake) =>
      log.info("Connecting to {}: {}({})",
        uri,
        handshake.getHttpStatusMessage,
        handshake.getHttpStatus
      )

      if(local.forwarding) {
        context.parent ! Register(self)
      } else {
        local.conn ! Register(self, keepOpenOnPeerClosed = true)
      }
      context become connected
  }

  private def connected: Receive = {
    case Received(bytes) if sender() == local.conn =>
      send(codec.encode(Plain(bytes)).bytes.toArray)

    case WSByteMessage(bytes) =>
      local.conn ?! Write(codec.decode(Encoded(bytes)).bytes, Ack)

    case (PeerClosed | ConfirmedClosed) if sender() == local.conn =>
      log.info("Instigator peer closed.")
      setLocalClosed()
      stopIfFullyClosedElse {
        closeBlocking()
      }
  }

  override def unhandled(message: Any): Unit = {
    message match {
      case WSClose(code, reason, remote) =>
        if(remote) {
          log.info("Remote peer closed: {}({})", reason, code)
        }
        else {
          log.info("Connection closed: {}({})", reason, code)
        }
        setRemoteClosed()
        stopIfFullyClosedElse {
          local.conn ?! ConfirmedClose
        }

      case WSError(ex) =>
        log.error(ex, "An error occurred in transfer")
        context stop self

      case _ => super.unhandled(message)
    }
  }
}

object WSRelay {
  def mkActor(local: Local, uri: URI, codec: Codec)
             (implicit context: ActorContext): ActorRef = {
    val clazz = classOf[WSRelay]
    val name = mkActorName(clazz, ":", local.conn, "~", uri)
    val p = Props.create(clazz, local, uri, codec)
    context.watch(context.actorOf(p, name))
  }
}
