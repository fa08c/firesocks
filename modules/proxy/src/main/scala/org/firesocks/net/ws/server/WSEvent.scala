package org.firesocks.net.ws.server

import akka.util.ByteString
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake

sealed abstract class WSEvent extends Serializable

@SerialVersionUID(1)
case class WSError(conn: WebSocket, ex: Exception) extends WSEvent

@SerialVersionUID(1)
case class WSStringMessage(conn: WebSocket, message: String) extends WSEvent

@SerialVersionUID(1)
case class WSByteMessage(conn: WebSocket, message: ByteString) extends WSEvent

@SerialVersionUID(1)
case class WSClose(conn: WebSocket, code: Int, reason: String, remote: Boolean)
  extends WSEvent

@SerialVersionUID(1)
case class WSOpen(conn: WebSocket, handshake: ClientHandshake) extends WSEvent

