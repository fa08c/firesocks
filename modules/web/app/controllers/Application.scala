package controllers

import play.api._
import play.api.mvc._
import play.api.Play.current
import relay.WSWorker

object Application extends Controller {

  def index = Action {
    Ok("<h3>It works!</h3>")
  }

  def worker = WebSocket.acceptWithActor[Array[Byte], Array[Byte]] {
    req => out =>
      Logger.info(s"Incoming connection from ${req.remoteAddress}")
      WSWorker.mkProps(out)
  }
}
