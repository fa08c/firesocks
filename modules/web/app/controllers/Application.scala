package controllers

import play.api._
import play.api.mvc._
import play.api.Play.current
import relay.WSWorker

object Application extends Controller {

  def index = Action {
    Redirect("http://www.yahoo.com")
  }

  def worker = WebSocket.acceptWithActor[Array[Byte], Array[Byte]] {
    req => out => WSWorker.mkProps(out)
  }
}
