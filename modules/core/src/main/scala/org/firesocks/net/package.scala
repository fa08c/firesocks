package org.firesocks

import java.net.{InetAddress, InetSocketAddress}
import java.nio.ByteOrder

package object net {
  implicit val NETWORK_ENDIAN = ByteOrder.BIG_ENDIAN

  def mkAddr(addr: String, port: Int) = new InetSocketAddress(addr, port)
  def mkAddr(addr: InetAddress, port: Int) = new InetSocketAddress(addr, port)

  val NULL_ADDR = mkAddr("0.0.0.0", 0)
}
