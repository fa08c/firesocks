package org.firesocks.net.ws.server

import org.java_websocket.WebSocket

import akka.actor._
import akka.io.Tcp
import akka.util.ByteString

import org.firesocks.util.Logger
import org.firesocks.codec.{Encoded, Plain, Codec}
import org.firesocks.net.tcp.{Ack, TCPRelay}
import org.firesocks.net.{Response1, Socks, Request1, Forwarded}
import org.firesocks.net._
import org.firesocks.lang._

class WSWorker(conn: WebSocket, codec: Codec) extends Actor with Logger {
  override def receive: Receive = {
    case Forwarded(bytes: ByteString, _) =>
      log.info("Initial request received.")
      codec.encode(Plain(bytes)).bytes match {
        case Request1(ver @ Socks.VER, cmd @ Socks.CMD_CONNECT, dst) =>
          log.info("Processing CONNECT to {}", dst)
          TCPRelay.mkActor(self, dst, codec)
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
      log.info("Relay {} terminated.", relay.path.name)
      context stop self

    case Forwarded(bytes: ByteString, _) =>
      relay.forward(Forwarded(Tcp.Received(bytes), self))

    case msg: Tcp.Write =>
      conn.send(msg.data.toArray)
      sender() ! Ack
  }
}

object WSWorker {
  def mkActor(conn: WebSocket, codec: Codec)
             (implicit context: ActorContext): ActorRef = {
    val clazz = classOf[WSWorker]
    val name = mkActorName(clazz)
    val p = Props.create(clazz, conn, codec)
    context.actorOf(p)
  }
}