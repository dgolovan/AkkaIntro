package com.example

import akka.actor._
import com.example.SessionActor.AddRequest
import com.example.events.{Request, Session}

import scala.collection.mutable.{ListBuffer}

class SessionActor(session: Session) extends Actor with ActorLogging {

  private[example] val requestHistory: ListBuffer[Request] = ListBuffer()

  def receive = {
    case AddRequest(request) =>

      requestHistory += request
      log.debug(requestHistory.toString)
  }
}

object SessionActor {
  case class AddRequest(request: Request)

  def props(session: Session): Props = {
    Props(new SessionActor(session))
  }
}