package relay

import akka.actor.{Terminated, Props, ActorRef, Actor}
import akka.io.Tcp
import akka.util.ByteString
import org.firesocks.app.{Config, Parser}
import org.firesocks.codec.{Encoded, Plain, Codec}
import org.firesocks.net.tcp.{Ack, TCPRelay}
import org.firesocks.net._
import org.firesocks.util.Logger

import scala.tools.cmd.CommandLineParser

class WSWorker(instigator: ActorRef, codec: Codec) extends Actor with Logger {
  private implicit val TCP_TIMEOUT = tcp.TIMEOUT

  override def postStop(): Unit = {
    log.info("Connection closed.")
  }

  override def receive: Receive = {
    case bytes: Array[Byte] =>
      log.info("Initial request received.")
      codec.inverse.encode(Plain(ByteString(bytes))).bytes match {
        case Request1(ver, cmd, dst)
          if ver == Socks.VER && cmd == Socks.CMD_CONNECT =>
          log.info("Processing CONNECT to {}", dst)
          TCPRelay.mkActor(Local(self), dst, codec.inverse)
          context become connectingTCP
      }
  }

  private def connectingTCP: Receive = {
    case msg: Tcp.Register =>
      log.info("Connected to remote.")
      val resp = Response1(Socks.VER, 0x00, NULL_ADDR)
      instigator ! codec.inverse.decode(Encoded(resp.toBytes)).bytes.toArray
      context.become(connectedTCP(msg.handler))
  }

  private def connectedTCP(relay: ActorRef): Receive = {
    case Terminated(actor) if actor == relay =>
      log.info("Relay {} terminated.", relay.path.name)
      context stop self

    case bytes: Array[Byte] =>
      relay ! Tcp.Received(ByteString(bytes))

    case msg: Tcp.Write =>
      instigator ! msg.data.toArray
      sender() ! Ack
  }

  override def unhandled(message: Any): Unit = {
    log.warning("Unhandled '{}' from {}", message, sender())
    super.unhandled(message)
  }
}

object WSWorker {
  val PROP_CMDLINE = "firesocks.cmdline"

  System.getProperty(PROP_CMDLINE) match {
    case null =>
      play.api.Logger.error(s"Property $PROP_CMDLINE does not exist!")
    case cmdline =>
      val args = CommandLineParser.tokenize(cmdline)
      Parser.parse(args, Config()) match {
        case Some(c) if c.mode == 'server => Config.set(c)
        case Some(c) =>
          val err = s"Invalid $PROP_CMDLINE: server mode config required."
          play.api.Logger.error(err)
        case None =>
          val err = s"Failed to parse $PROP_CMDLINE: $cmdline"
          play.api.Logger.error(err)
      }
  }

  def mkProps(instigator: ActorRef): Props = {
    Props(classOf[WSWorker], instigator, Config.get.codec())
  }
}
