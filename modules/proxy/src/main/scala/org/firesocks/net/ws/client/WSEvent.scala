package org.firesocks.net.ws.client

import akka.util.ByteString
import org.java_websocket.handshake.ServerHandshake

sealed abstract class WSEvent extends Serializable

@SerialVersionUID(1)
case class WSError(ex: Exception) extends WSEvent

@SerialVersionUID(1)
case class WSStringMessage(message: String) extends WSEvent

@SerialVersionUID(1)
case class WSByteMessage(message: ByteString) extends WSEvent

@SerialVersionUID(1)
case class WSClose(code: Int, reason: String, remote: Boolean)
  extends WSEvent

@SerialVersionUID(1)
case class WSOpen(handshake: ServerHandshake) extends WSEvent
