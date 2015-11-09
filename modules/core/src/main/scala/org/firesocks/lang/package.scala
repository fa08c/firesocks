package org.firesocks

import java.nio.charset.Charset
import java.net.{URI, InetSocketAddress, InetAddress}

import akka.util.{Timeout, ByteIterator}

import scala.concurrent.Await
import scala.concurrent.duration._

import akka.actor.ActorRef
import akka.pattern.ask

import language.reflectiveCalls
import language.postfixOps

package object lang {
  val CHARSET_UTF8 = Charset.forName("UTF-8")

  def quietly[R](code: => R): Unit = {
    try {
      code
    }
    catch {
      case e: Exception => // pass
    }
  }

  def using[C <: {def close(): Unit}, R](closable: C)(process: C => R): R = {
    try {
      process(closable)
    }
    finally {
      closable.close()
    }
  }

  implicit class AsWord[T](value: T) {
    def as[R](code: T => R): R = code(value)
  }

  implicit class ByteStringIteratorEx(itr: ByteIterator) {
    def getBytes(n: Int): Array[Byte] = {
      val rv = new Array[Byte](n)
      itr.getBytes(rv)
      rv
    }
  }

  implicit class ItrEx[T](itr: Iterator[T]) {
    def next2(): (T, T) = (itr.next(), itr.next())
    def next3(): (T, T, T) = (itr.next(), itr.next(), itr.next())
    def next4(): (T, T, T, T) = (itr.next(), itr.next(), itr.next(), itr.next())
    def next5(): (T, T, T, T, T) =
      (itr.next(), itr.next(), itr.next(), itr.next(), itr.next())
    def next6(): (T, T, T, T, T, T) =
      (itr.next(), itr.next(), itr.next(), itr.next(), itr.next(), itr.next())
    def next7(): (T, T, T, T, T, T, T) =
      (itr.next(), itr.next(), itr.next(), itr.next(), itr.next(), itr.next(),
        itr.next())
    def next8(): (T, T, T, T, T, T, T, T) =
      (itr.next(), itr.next(), itr.next(), itr.next(), itr.next(), itr.next(),
        itr.next(), itr.next())
  }

  implicit class AskEx(actor: ActorRef)(implicit timeout: FiniteDuration) {
    private implicit val ASK_TIMEOUT: Timeout = timeout

    def ?! (message: Any): Any = {
      val f = actor ? message
      Await.result(f, timeout)
    }
  }

  def mkActorName(token: Any*): String = token.map {
    case c: Class[_] => c.getSimpleName
    case a: ActorRef => a.path.name
    case a: InetAddress => a.toString.replace('/', ';')
    case a: InetSocketAddress => a.toString.replace('/', ';')
    case u: URI => u.toString.replace('/', ';')
    case s => s.toString
  }.mkString
}
