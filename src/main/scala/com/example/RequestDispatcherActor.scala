package com.example

import akka.actor._
import akka.event.LoggingReceive
import com.example.RequestDispatcherActor.DispatchRequests
import com.example.events.{Request, Session}

import scala.collection.mutable.{Map => MutableMap}
import scala.concurrent.duration._

/**
  * Created by denisg on 2016-03-17.
  */
class RequestDispatcherActor(statsActor: ActorRef) extends Actor with ActorLogging {

  private val sessionMap: MutableMap[Session, ActorRef] = MutableMap()
  private val timeoutDuration: FiniteDuration = context.system.settings.config.getDuration("main.session-timeout", MILLISECONDS).millis


  override def receive: Receive = LoggingReceive {
    case DispatchRequests(requests) =>
      requests.foreach { request =>
        val sessionActor = sessionMap.getOrElseUpdate(request.session, createSessionActor(request.session))
        sessionActor ! SessionActor.AddRequest(request)
      }

    case Terminated(sessionActor) =>

      sessionMap.find { case (k, v) => v == sessionActor }.foreach {
        case (deadSession, _) =>
          log.debug("Removing dead session actor: {}", deadSession)
          sessionMap -= deadSession
      }
  }

  private[example] def createSessionActor(session: Session): ActorRef = {
    val sessionActor = context.actorOf(SessionActor.props(session, timeoutDuration, statsActor))
    context.watch(sessionActor)
    sessionActor
  }
}

object RequestDispatcherActor {

  case class DispatchRequests(requests: List[Request])

  def props(statsActor: ActorRef): Props = {
    Props(new RequestDispatcherActor(statsActor))
  }


}