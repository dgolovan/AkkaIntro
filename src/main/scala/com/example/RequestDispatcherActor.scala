package com.example

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.example.RequestDispatcherActor.DispatchRequests
import com.example.events.{Request, Session}

import scala.collection.mutable.{Map => MutableMap}

/**
  * Created by denisg on 2016-03-17.
  */
class RequestDispatcherActor extends Actor with ActorLogging {

  private val sessionMap: MutableMap[Session, ActorRef] = MutableMap()

  def receive = {
    case DispatchRequests(requests) =>
      requests.foreach { request =>
        val sessionActor = sessionMap.getOrElseUpdate(request.session, createSessionActor(request.session))
        sessionActor ! SessionActor.AddRequest(request)
      }
  }

  private[example] def createSessionActor(session: Session): ActorRef = {
    context.actorOf(SessionActor.props(session))
  }
}

object RequestDispatcherActor {

  case class DispatchRequests(requests: List[Request])

  def props: Props = {
    Props(new RequestDispatcherActor)
  }


}