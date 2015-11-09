package org.firesocks.app

import akka.actor.{Props, ActorSystem}
import com.typesafe.config.ConfigFactory

import org.firesocks.lang._
import org.firesocks.util.UnhandledMessageListener

trait TApp extends App {
  lazy val system = Config.get as { c =>
    val conf = ConfigFactory.parseString(s"""
    |akka.loglevel = ${c.verbose}
    """.stripMargin)
    val rv = ActorSystem("default", conf)
    rv.actorOf(Props[UnhandledMessageListener])
    rv
  }
}
