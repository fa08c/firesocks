package org.firesocks.net.ws.server

import java.net.InetSocketAddress
import java.nio.ByteBuffer

import akka.actor.{Terminated, Actor}
import akka.util.ByteString
import org.firesocks.util.Logger
import org.java_websocket.WebSocket
import org.java_websocket.drafts.{Draft_76, Draft_75, Draft_10, Draft_17}
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer

abstract class WSServer(bind: InetSocketAddress) extends WebSocketServer(
  bind, WebSocketServer.DECODERS, java.util.Arrays.asList(
    new Draft_10(),
    new Draft_17(),
    new Draft_75(),
    new Draft_76()
  )) with Actor with Logger {

  private var terminated = false

  override def postStop(): Unit = {
    terminated = true
    stop()
  }

  override def onError(conn: WebSocket, ex: Exception): Unit = {
    tellNicely(WSError(conn, ex))
  }

  override def onMessage(conn: WebSocket, message: String): Unit = {
    tellNicely(WSStringMessage(conn, message))
  }

  override def onMessage(conn: WebSocket, message: ByteBuffer): Unit = {
    tellNicely(WSByteMessage(conn, ByteString(message)))
  }

  override def onClose(conn: WebSocket,
                       code: Int,
                       reason: String,
                       remote: Boolean): Unit = {
    tellNicely(WSClose(conn, code, reason, remote), warn = false)
  }

  override def onOpen(conn: WebSocket, handshake: ClientHandshake): Unit = {
    tellNicely(WSOpen(conn, handshake))
  }

  override def unhandled(message: Any): Unit = {
    message match {
      case Terminated(actor) if actor == self =>
        terminated = true
      case _ => super.unhandled(message)
    }
  }

  private def tellNicely(event: WSEvent, warn: Boolean=true): Unit = {
    if(!terminated) {
      self ! event
    } else if(warn) {
      log.warning("Event {} ignored in termination", event)
    }
  }
}
