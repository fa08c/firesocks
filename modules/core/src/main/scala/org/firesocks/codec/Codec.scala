package org.firesocks.codec

import akka.util.ByteString

case class Plain(bytes: ByteString)

case class Encoded(bytes: ByteString)

trait Codec {
  def encode(data: Plain): Encoded
  def decode(data: Encoded): Plain

  lazy val inverse: Codec = this match {
    case InverseCodec(orig) => orig
    case _ => InverseCodec(this)
  }
}

case class InverseCodec(orig: Codec) extends Codec {
  def encode(data: Plain): Encoded =
    Encoded(orig.decode(Encoded(data.bytes)).bytes)

  def decode(data: Encoded): Plain =
    Plain(orig.encode(Plain(data.bytes)).bytes)
}

object Codec {
  val IDENTITY = new Codec {
    override def encode(data: Plain): Encoded = Encoded(data.bytes)
    override def decode(data: Encoded): Plain = Plain(data.bytes)
  }
}
