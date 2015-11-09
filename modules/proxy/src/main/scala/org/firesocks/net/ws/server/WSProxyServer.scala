package org.firesocks.net.ws.server

import org.java_websocket.WebSocket

import akka.actor.{Props, ActorSystem, Terminated, ActorRef}
import org.firesocks.app.Config
import org.firesocks.net.Forwarded

import org.firesocks.lang._

class WSProxyServer(config: Config) extends WSServer(config.bind) {
  require(config.mode == 'server)

  private var connsToWorkers: Map[WebSocket, ActorRef] = Map()
  private var workersToConns: Map[ActorRef, WebSocket] = Map()

  start()

  override def receive: Receive = {
    case WSError(conn, ex) =>
      log.error(ex, "An error occurred in transfer")
      connsToWorkers.get(conn).foreach { context.stop }

    case WSOpen(conn, handshake) =>
      log.info("Client connecting {}", handshake.getResourceDescriptor)

      val worker = WSWorker.mkActor(conn, config.codec().inverse)
      connsToWorkers += conn -> worker
      workersToConns += worker -> conn

    case WSStringMessage(conn, message) =>
      connsToWorkers.get(conn) match {
        case Some(worker) =>
          worker.forward(Forwarded(message, self))
        case None =>
          log.warning("Received from dangling connection, closing.")
          conn.close()
      }

    case WSByteMessage(conn, message) =>
      connsToWorkers.get(conn) match {
        case Some(worker) =>
          worker.forward(Forwarded(message, self))
        case None =>
          log.warning("Received from dangling connection, closing.")
          conn.close()
      }

    case WSClose(conn, code, reason, remote) =>
      if(remote) {
        log.info("Client closed: {} ({})", reason, code)
      } else {
        log.info("Connection closed: {} ({})", reason, code)
      }

      if(connsToWorkers.contains(conn)) {
        val worker = connsToWorkers(conn)
        workersToConns -= worker
        connsToWorkers -= conn

        context unwatch worker
        context stop worker
      }

    case Terminated(actor) if actor != self =>
      log.info("Worker {} terminated.", actor.path.name)
      if(workersToConns.contains(actor)) {
        val conn = workersToConns(actor)
        workersToConns -= actor
        connsToWorkers -= conn

        conn.close()
      }
  }
}

object WSProxyServer {
  def mkActor(system: ActorSystem, config: Config): ActorRef = {
    val clazz = classOf[WSProxyServer]
    val name = mkActorName(clazz, "@", config.bind)
    val p = Props.create(clazz, config)
    system.actorOf(p, name)
  }
}
