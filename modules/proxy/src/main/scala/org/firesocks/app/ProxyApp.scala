package org.firesocks.app

import org.firesocks.proxy.Proxy
import org.firesocks.net.ws.server.WSProxyServer

object ProxyApp extends TApp {

  Parser.parse(args, Config()) match {
    case Some(c) if c.mode == 'proxy =>
      Config.set(c)
      Proxy.mkActor(system, c)
    case Some(c) if c.mode == 'server =>
      Config.set(c)
      WSProxyServer.mkActor(system, c)
    case None =>
      System.exit(1)
  }
}
