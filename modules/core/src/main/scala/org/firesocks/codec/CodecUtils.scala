package org.firesocks.codec

import java.security.{MessageDigest, Key}
import javax.crypto.spec.SecretKeySpec

import akka.util.ByteString
import org.firesocks.lang

import scala.collection.mutable.ArrayBuffer

object CodecUtils {
  val MD5 = "MD5"

  private var cachedKeys: Map[String, (ByteString, ByteString)] = Map()

  /**
    * Equivalent of OpenSSL's EVP_BytesToKey() with count 1
    * so that we make the same key and iv as nodejs version
    */
  def mkKey(secret: String,
            nKeyBits: Int,
            nIVBits: Int): (ByteString, ByteString) = {
    require(nKeyBits >= 0 && nIVBits >= 0)

    val (keyLen: Int, ivLen: Int) = (nKeyBits / 8, nIVBits / 8)

    val k = s"$secret-$keyLen-$ivLen"
    cachedKeys.get(k) match {
      case Some(rv) => rv
      case None =>
        val minLen = keyLen + ivLen
        val md5 = MessageDigest.getInstance(MD5)

        var m = Vector[Array[Byte]]()
        var l = 0
        do {
          if(m.nonEmpty) {
            md5.update(m.last)
          }
          md5.update(secret.getBytes(lang.CHARSET_UTF8))
          m :+= md5.digest()
          l = m.foldLeft(0){ (len, bytes) => len + bytes.length }
        } while(l < minLen)

        val ms = m.foldLeft(new ArrayBuffer[Byte](l)){ (buf, bytes) =>
          buf ++= bytes
        }

        val keyBytes = new Array[Byte](keyLen)
        ms.copyToArray(keyBytes)

        val ivBytes = new Array[Byte](ivLen)
        ms.copyToArray(ivBytes, keyLen)

        val v = (ByteString(keyBytes), ByteString(ivBytes))
        cachedKeys += k -> v
        v
    }
  }
}
