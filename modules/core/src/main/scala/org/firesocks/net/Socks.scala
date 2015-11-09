package org.firesocks.net

import java.net.{Inet4Address, Inet6Address, InetAddress, InetSocketAddress}

import akka.util.{ByteStringBuilder, ByteString}

import org.firesocks.lang._

object Socks {
  val VER: Byte = 0x5

  val CMD_CONNECT = 0x01
  val CMD_BIND = 0x02
  val CMD_UDP_ASSOC = 0x03
}

/**
  * RFC 1928
  * +----+----------+----------+
  * |VER | NMETHODS | METHODS  |
  * +----+----------+----------+
  * | 1  |    1     | 1 to 255 |
  * +----+----------+----------+
  */
object Request0 {
  def unapply(bytes: ByteString): Option[(Byte, Byte, Vector[Byte])] = {
    val itr = bytes.iterator
    itr.next2() match {
      case (ver, nMethods) if ver == Socks.VER =>
        Some((ver, nMethods, itr.getBytes(nMethods).toVector))
      case _ => None
    }
  }
}

/**
  * RFC 1928
  * +----+--------+
  * |VER | METHOD |
  * +----+--------+
  * | 1  |   1    |
  * +----+--------+
  */
case class Response0(ver: Byte, method: Byte) {
  def toBytes: ByteString = ByteString(ver, method)
}

/**
  * RFC 1928
  * +----+-----+-------+------+----------+----------+
  * |VER | CMD |  RSV  | ATYP | DST.ADDR | DST.PORT |
  * +----+-----+-------+------+----------+----------+
  * | 1  |  1  | X'00' |  1   | Variable |    2     |
  * +----+-----+-------+------+----------+----------+
  */
object Request1 {
  def unapply(bytes: ByteString): Option[(Byte, Byte, InetSocketAddress)] = {
    val itr = bytes.iterator
    itr.next4() match {
      case (ver, cmd, rsv, atyp) if ver == Socks.VER && rsv == 0x0 =>
        val dstAddr = atyp match {
          case 0x1 => // IPv4
            InetAddress.getByAddress(itr.getBytes(4))
          case 0x3 => // name
            val n = itr.next()
            InetAddress.getByName(new String(itr.getBytes(n), CHARSET_UTF8))
          case 0x4 => // IPv6
            InetAddress.getByAddress(itr.getBytes(16))
        }
        val dstPort = itr.getShort & 0xFFFF
        Some((ver, cmd, new InetSocketAddress(dstAddr, dstPort)))
      case _ => None
    }
  }
}

/**
  * RFC 1928
  * +----+-----+-------+------+----------+----------+
  * |VER | REP |  RSV  | ATYP | BND.ADDR | BND.PORT |
  * +----+-----+-------+------+----------+----------+
  * | 1  |  1  | X'00' |  1   | Variable |    2     |
  * +----+-----+-------+------+----------+----------+
  */
case class Response1(ver: Byte, rep: Byte, bnd: InetSocketAddress) {
  def toBytes: ByteString = {
    val b = new ByteStringBuilder()
    b ++= Seq(ver, rep, 0)

    val (bndAddr, bndPort) = (bnd.getAddress, bnd.getPort)
    bndAddr match {
      case _: Inet4Address =>
        b += 0x1
        b ++= bndAddr.getAddress
      case _: Inet6Address =>
        b += 0x4
        b ++= bndAddr.getAddress
      case _ =>
        b += 0x3
        val name = bndAddr.getHostName
        val n = name.length ensuring (_ < 128)
        b += n.toByte
        b ++= name.getBytes(CHARSET_UTF8)
    }

    b.putShort(bndPort)
    b.result()
  }
}

