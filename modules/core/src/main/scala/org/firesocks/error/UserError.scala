package org.firesocks.error

case class UserError(key: String, args: Any*) extends RuntimeException(key)

case class ServerError(key: String, args: Any*)(cause: Throwable = null)
  extends RuntimeException(key, cause)
