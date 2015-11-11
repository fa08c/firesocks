package org.firesocks.proxy

import java.net._
import java.util.concurrent.ThreadLocalRandom

import akka.actor._
import akka.io.Tcp
import akka.util.ByteString

import org.firesocks.app.Config
import org.firesocks.codec.Codec
import org.firesocks.lang._
import org.firesocks.net._
import org.firesocks.net.tcp.{Ack, TCPRelay}
import org.firesocks.net.ws.client.WSRelay
import org.firesocks.util.Logger

class Broker(client: ActorRef,
             server: Either[InetSocketAddress, URI],
             codec: Codec,
             bndAddr: InetAddress) extends Actor with Logger {

  private implicit val TCP_TIMEOUT = tcp.TIMEOUT

  override def postStop(): Unit = {
    log.info("Broker stopped.")
  }

  override def receive: Receive = stage0

  private def stage0: Receive = {
    case Tcp.Received(bytes: ByteString) =>
      bytes match {
        case Request0(ver, _, _) if ver == Socks.VER =>
          client ?! Tcp.Write(Response0(ver, 0).toBytes, Ack)
          context become stage1

        case req =>
          log.error("Invalid request0 {}", req)
          context stop self
      }
  }

  // This stage is here, because decision about how to connect to the
  // server may be made based on the request (e.g. we may want to use UDP
  // instead of TCP, if the request is a UDP_ASSOC.
  // BUT, is it really necessary?
  private def stage1: Receive = {
    case Tcp.CommandFailed(_: Tcp.Write) =>
      log.error("Failed to write response0")
      context stop self

    case Tcp.Received(bytes: ByteString) =>
      bytes match {
        case Request1(ver, cmd, dst) if ver == Socks.VER =>
          cmd match {
            case _ @ Socks.CMD_CONNECT =>
              server match {
                case Left(addr) =>
                  log.info("Processing CONNECT to {} via {}", dst, addr)
                  TCPRelay.mkActor(Local(client, forwarding = true), addr, codec)
                case Right(uri) =>
                  log.info("Processing CONNECT to {} via {}", dst, uri)
                  WSRelay.mkActor(Local(client, forwarding = true), uri, codec)
              }
              context become connectingTCP(bytes)

            case _ @ Socks.CMD_UDP_ASSOC =>
              log.info("Processing UDP_ASSOC to {}", dst)
              // TODO
              context stop self

            case _ @ Socks.CMD_BIND =>
              log.warning("Processing BIND to {}", dst)
              context stop self
          }

        case req =>
          log.error("Invalid request1 {}", req)
          context stop self
      }
  }

  private def connectingTCP(req: ByteString): Receive = {
    case msg: Tcp.Register =>
      log.info("Sending initial request to remote proxy server.")

      val relay = msg.handler
      relay.tell(Tcp.Received(req), client)

      // NB The server will be responsible to reply to the request0, because
      //    not until then the server will be ready.
      // NB Not as is described in RFC 1928, bnd is actually ignored
      //    by the client. So it seems not important what the server returns
      //    as bnd.

      context.become(connectedTCP(relay))
  }

  private def connectedTCP(relay: ActorRef): Receive = {
    case Terminated(actor) if relay == actor =>
      context stop self
    case msg: Tcp.Received => relay.forward(msg)
    case msg @ (Tcp.PeerClosed | Tcp.ConfirmedClosed) => relay.forward(msg)
  }

  override def unhandled(message: Any): Unit = {
    message match {
      case Tcp.PeerClosed =>
        log.info("Closing before connected.")
        context stop self

      case _ => super.unhandled(message)
    }
  }
}

object Broker {
  def mkActor(client: ActorRef, config: Config)
             (implicit context: ActorContext): ActorRef = {
    val serverIndex = chooseServer(config)
    val server = config.servers(serverIndex)
    val codec = config.codec()
    val bndAddr = config.bind.getAddress
    val clazz = classOf[Broker]
    val name = mkActorName(clazz, ":", client, "$", serverIndex)
    val p = Props.create(clazz, client, server, codec, bndAddr)
    context.actorOf(p, name)
  }

  private def chooseServer(config: Config): Int = {
    val servers = config.servers
    ThreadLocalRandom.current().nextInt(servers.size)
  }
}
