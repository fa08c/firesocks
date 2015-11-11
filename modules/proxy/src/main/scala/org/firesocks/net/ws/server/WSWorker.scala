package org.firesocks.net.ws.server

import akka.io.Tcp.ConfirmedClose
import org.java_websocket.WebSocket

import akka.actor._
import akka.io.Tcp
import akka.util.ByteString

import org.firesocks.util.Logger
import org.firesocks.codec.{Encoded, Plain, Codec}
import org.firesocks.net.tcp.{Ack, TCPRelay}
import org.firesocks.net.{Response1, Socks, Request1}
import org.firesocks.net._
import org.firesocks.lang._

class WSWorker(conn: WebSocket, codec: Codec) extends Actor with Logger {
  override def postStop(): Unit = {
    log.info("Worker stopped.")
  }

  override def receive: Receive = {
    case bytes: ByteString =>
      log.info("Initial request received.")
      codec.encode(Plain(bytes)).bytes match {
        case Request1(ver @ Socks.VER, cmd @ Socks.CMD_CONNECT, dst) =>
          log.info("Processing CONNECT to {}", dst)
          TCPRelay.mkActor(Local(self), dst, codec)
          context become connectingTCP
      }
  }

  private def connectingTCP: Receive = {
    case msg: Tcp.Register =>
      log.info("Connected to remote.")
      val resp = Response1(Socks.VER, 0x00, NULL_ADDR)
      conn.send(codec.decode(Encoded(resp.toBytes)).bytes.toArray)
      context.become(connectedTCP(msg.handler))
  }

  private def connectedTCP(relay: ActorRef): Receive = {
    case Terminated(actor) if actor == relay =>
      context stop self

    case bytes: ByteString =>
      relay ! Tcp.Received(bytes)

    case msg: Tcp.Write =>
      conn.send(msg.data.toArray)
      sender() ! Ack

    case msg @ ConfirmedClose =>
      conn.close()
      sender() ! msg.event

    case msg: WSClose =>
      relay ! Tcp.PeerClosed
  }

  override def unhandled(message: Any): Unit = {
    message match {
      case msg: WSClose =>
        log.info("Closing before connected.")
        context stop self
      case _ => super.unhandled(message)
    }
  }
}

object WSWorker {
  def mkActor(conn: WebSocket, codec: Codec)
             (implicit context: ActorContext): ActorRef = {
    val clazz = classOf[WSWorker]
    val name = mkActorName(clazz, ":", conn.getRemoteSocketAddress)
    val p = Props.create(clazz, conn, codec)
    context.actorOf(p, name)
  }
}