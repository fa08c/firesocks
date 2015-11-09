package org.firesocks.codec

import javax.crypto.spec.SecretKeySpec
import javax.crypto.Cipher

import akka.util.ByteString

class BuiltinCodec(secret: String,
                   algorithm: String,
                   keyLen: Int = 128,
                   ivLen: Int = 0) extends Codec {
  val keyAlgorithm = algorithm.split("/", 2)(0)

  private val (keyBytes, ivBytes) = CodecUtils.mkKey(secret, keyLen, ivLen)
  private val key = new SecretKeySpec(keyBytes.toArray, keyAlgorithm)

  private val cipherEnc = Cipher.getInstance(algorithm)
  cipherEnc.init(Cipher.ENCRYPT_MODE, key)

  private val cipherDec = Cipher.getInstance(algorithm)
  cipherDec.init(Cipher.DECRYPT_MODE, key)

  override def encode(data: Plain): Encoded = {
    Encoded(ByteString(cipherEnc.doFinal(data.bytes.toArray)))
  }

  override def decode(data: Encoded): Plain = {
    Plain(ByteString(cipherDec.doFinal(data.bytes.toArray)))
  }
}

object BuiltinCodec {
  val DEFAULT_KEY_LEN = 128
  val DEFAULT_IV_LEN = 0
}
