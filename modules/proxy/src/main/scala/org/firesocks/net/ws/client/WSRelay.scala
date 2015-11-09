package org.firesocks.net.ws.client

import java.net.URI

import akka.actor.{Actor, ActorContext, ActorRef, Props}
import akka.io.Tcp
import org.firesocks.codec.{Codec, Encoded, Plain}
import org.firesocks.lang._
import org.firesocks.net._
import org.firesocks.net.tcp.Ack

class WSRelay(instigator: ActorRef,
              uri: URI,
              codec: Codec) extends WSClient(uri) {
  import Tcp._

  private implicit val TCP_TIMEOUT = tcp.TIMEOUT

  override def receive: Actor.Receive = {
    case WSOpen(handshake) =>
      log.info("Connecting to {}: {}({})",
        uri,
        handshake.getHttpStatusMessage,
        handshake.getHttpStatus
      )

      instigator ! Register(self)
      context become connected
  }

  private def connected: Receive = {
    case Forwarded(Received(bytes), via) if via == instigator =>
      send(codec.encode(Plain(bytes)).bytes.toArray)

    case Received(bytes) if sender() == instigator =>
      send(codec.encode(Plain(bytes)).bytes.toArray)

    case WSByteMessage(bytes) =>
      instigator ?! Write(codec.decode(Encoded(bytes)).bytes, Ack)

    case PeerClosed if sender() == instigator =>
      log.info("Instigator peer closed.")
      context stop self

    case WSClose(code, reason, remote) =>
      if(remote) {
        log.info("Remote peer closed: {}({})", reason, code)
      }
      else {
        log.info("Connection closed: {}({})", reason, code)
      }
      context stop self
  }

  override def unhandled(message: Any): Unit = {
    message match {
      case WSError(ex) =>
        log.error(ex, "An error occurred in transfer")
        context stop self
      case _ => super.unhandled(message)
    }
  }
}

object WSRelay {
  def mkActor(instigator: ActorRef, uri: URI, codec: Codec)
             (implicit context: ActorContext): ActorRef = {
    val clazz = classOf[WSRelay]
    val name = mkActorName(clazz, ":", instigator, "~", uri)
    val p = Props.create(clazz, instigator, uri, codec)
    context.actorOf(p, name)
  }
}
