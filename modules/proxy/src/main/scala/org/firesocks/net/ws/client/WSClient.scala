package org.firesocks.net.ws.client

import java.net.URI
import java.nio.ByteBuffer

import akka.actor.{Actor, Terminated}
import akka.util.ByteString
import org.firesocks.util.Logger
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_17
import org.java_websocket.handshake.ServerHandshake

abstract class WSClient(uri: URI)
  extends WebSocketClient(uri, new Draft_17()) with Actor with Logger {

  @volatile
  private var terminated = false

  context watch self

  connect()

  override def postStop(): Unit = {
    terminated = true
    close()
  }

  override def onError(ex: Exception): Unit = {
    tellNicely(WSError(ex))
  }

  override def onMessage(message: String): Unit = {
    tellNicely(WSStringMessage(message))
  }

  override def onMessage(bytes: ByteBuffer): Unit = {
    tellNicely(WSByteMessage(ByteString(bytes)))
  }

  override def onClose(code: Int, reason: String, remote: Boolean): Unit = {
    tellNicely(WSClose(code, reason, remote), warn = false)
  }

  override def onOpen(handshakedata: ServerHandshake): Unit = {
    tellNicely(WSOpen(handshakedata))
  }

  private def tellNicely(event: WSEvent, warn: Boolean = true): Unit = {
    if(!terminated) {
      self ! event
    } else if(warn) {
      log.warning("Event {} ignored in termination", event)
    }
  }
}
