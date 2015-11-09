package org.firesocks.app

import java.net.{URI, InetSocketAddress}
import scala.util.{Failure, Success, Try}

import org.firesocks.codec.{BuiltinCodec, Codec}
import org.firesocks.net._
import scopt.OptionDef

case class Config(mode: Symbol = 'proxy,
                  bind: InetSocketAddress = Config.DEFAULT_BIND,
                  servers: Vector[Either[InetSocketAddress, URI]] = Vector(),
                  codec: () => Codec = () => Codec.IDENTITY,
                  verbose: String = "info")

object Config {
  private var instance: Option[Config] = None

  def get = instance.get

  def set(config: Config): Unit = {
    instance = Option(config)
  }

  val DEFAULT_BIND = mkAddr("0.0.0.0", 1080)
  val DEFAULT_SERVER_PORT = 8080
}

object Parser extends scopt.OptionParser[Config]("firesocks") {
  head(programName, "0.1")

  cmd("server") text {
    s"Starts $programName in server mode."
  } action { (_, c) =>
    c.copy(mode='server)
  } children (optBind(),
    optCodec(),
    optVerbose())

  cmd("proxy") text {
    s"Starts $programName in proxy mode."
  } action { (_, c) =>
    c.copy(mode='proxy)
  } children (
    optBind(),
    optServer() required() unbounded(),
    optCodec(),
    optVerbose())

  checkConfig { c =>
    if(Seq("debug", "info", "warning").contains(c.verbose)) {
      success
    }
    else {
      failure(s"Invalid verbose level ${c.verbose}.")
    }
  }

  private def parseSockAddr(s: String,
                            defPort: Option[Int] = None): InetSocketAddress = {
    val parts = s.split(":")
    if(parts.length > 2 ) {
      val err = "Invalid address, expecting addr[:port]"
      throw new IllegalArgumentException(err)
    } else {
      val port = if(parts.length > 1) {
        try {
          Integer.parseInt(parts(1))
        } catch {
          case _: Exception => -1
        }
      } else defPort.getOrElse(-1)

      if (port <= 0 || port >= 0xFFFF) {
        val err = "Invalid port number, expecting number between [0, 65535]"
        throw new IllegalArgumentException(err)
      }

      mkAddr(parts(0), port)
    }
  }

  private def parseCodec(s: String,
                         defKL: Option[Int] = None): Codec = {
    val parts = s.split(":")
    if(parts.length > 3 || parts.length < 2) {
      val err = "Invalid codec, expecting password:algorithm[:keyLen]"
      throw new IllegalArgumentException(err)
    } else {
      val keyLen = if(parts.length == 3) {
        try {
          Integer.parseInt(parts(2))
        } catch {
          case e: NumberFormatException => -1
        }
      } else defKL.getOrElse(-1)

      if(keyLen <= 0) {
        throw new IllegalArgumentException("Invalid keyLen")
      }

      new BuiltinCodec(parts(0), parts(1), keyLen)
    }
  }

  private def optBind(): OptionDef[String, Config] = {
    opt[String]('b', "bind") text {
      "The address (addr[:port]) that the server / proxy binds to."
    } validate { s =>
      Try { parseSockAddr(s, Some(0)) } match {
        case Success(_) => success
        case Failure(t) => failure(t.getMessage)
      }
    } action { (x, c) =>
      c.copy(bind=parseSockAddr(x, Some(c.bind.getPort)))
    }
  }

  private def optServer(): OptionDef[String, Config] = {
    opt[String]('s', "server") text {
      "The server address (addr[:port]|uri) that proxy should connect to."
    } validate { s =>
      if(s.contains("://")) {
        Try { URI.create(s) } match {
          case Success(_) => success
          case Failure(_) => failure("Invalid URI")
        }
      } else {
        Try { parseSockAddr(s, Some(0)) } match {
          case Success(_) => success
          case Failure(t) => failure(t.getMessage)
        }
      }
    } action { (x, c) =>
      if(x.contains("://")) {
        c.copy(servers=c.servers :+ Right(URI.create(x)))
      } else {
        val addr = parseSockAddr(x, Some(c.bind.getPort))
        c.copy(servers=c.servers :+ Left(addr))
      }
    }
  }

  private def optCodec(): OptionDef[String, Config] = {
    opt[String]('c', "codec") text {
      "The codec (password:algorithm[:keyLen]) used in transport."
    } validate { s =>
      Try { parseCodec(s, Some(BuiltinCodec.DEFAULT_KEY_LEN))} match {
        case Success(_) => success
        case Failure(t) => failure(t.getMessage)
      }
    } action { (x, c) =>
      val codec = parseCodec(x, Some(BuiltinCodec.DEFAULT_KEY_LEN))
      c.copy(codec=()=>codec)
    }
  }

  private def optVerbose(): OptionDef[String, Config] = {
    opt[String]('v', "verbose") text {
      "The verbose level, one of (debug, info, warning)."
    } action { (x, c) =>
      c.copy(verbose=x)
    }
  }
}
