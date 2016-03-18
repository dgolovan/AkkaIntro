package com.example

import akka.actor._
import akka.event.LoggingReceive
import com.example.SessionActor.{InactivityTimeout, AddRequest}
import com.example.events.{Request, Session}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._

class SessionActor(session: Session, timeoutDuration: FiniteDuration, statsActor: ActorRef) extends Actor with ActorLogging {

  import context.dispatcher

  private var inactivityTimer: Cancellable = rescheduleInactivityTimeout()

  private[example] val requestHistory: ListBuffer[Request] = ListBuffer()

  override def receive: Receive = LoggingReceive {
    case AddRequest(request) =>
      requestHistory += request

      inactivityTimer.cancel()
      rescheduleInactivityTimeout()

      log.debug(requestHistory.toString)

    case InactivityTimeout =>
      statsActor ! StatsActor.AggregateStats(requestHistory.toList)
      context.stop(self)
  }

  private def rescheduleInactivityTimeout() = {
    inactivityTimer = context.system.scheduler.scheduleOnce(timeoutDuration, self, InactivityTimeout)
    inactivityTimer
  }
}

object SessionActor {

  case class AddRequest(request: Request)

  case object InactivityTimeout

  def props(session: Session, timeoutDuration: FiniteDuration, statsActor: ActorRef): Props = {
    Props(new SessionActor(session, timeoutDuration, statsActor))
  }

}