package com.example

import akka.actor._
import com.example.SessionActor.{InactivityTimeout, AddRequest}
import com.example.events.{Request, Session}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._

class SessionActor(session: Session, timeoutDuration: FiniteDuration) extends Actor with ActorLogging {
  import context.dispatcher

  private var inactivityTimer: Cancellable = rescheduleInactivityTimeout()

  private[example] val requestHistory: ListBuffer[Request] = ListBuffer()

  private val statsActor: ActorRef = createStatsActor()

  def receive = {
    case AddRequest(request) =>
      requestHistory += request

      inactivityTimer.cancel()
      rescheduleInactivityTimeout()

      log.debug(requestHistory.toString)

    case InactivityTimeout =>
      statsActor ! StatsActor.AggregateStats(requestHistory.toList)
      context.stop(self)
  }

  private[example] def createStatsActor(): ActorRef = {
    context.actorOf(Props(new StatsActor))
  }

  private def rescheduleInactivityTimeout() = {
    inactivityTimer = context.system.scheduler.scheduleOnce(timeoutDuration, self, InactivityTimeout)
    inactivityTimer
  }
}

object SessionActor {
  case class AddRequest(request: Request)
  case object InactivityTimeout

  def props(session: Session, timeoutDuration: FiniteDuration): Props = {
    Props(new SessionActor(session, timeoutDuration))
  }

}