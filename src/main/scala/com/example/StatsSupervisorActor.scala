package com.example

import akka.actor.SupervisorStrategy.{Decider, Resume}
import akka.actor._
import akka.event.LoggingReceive

/**
  * Created by denisg on 2016-03-17.
  */
class StatsSupervisorActor(maybeAccuracy: Option[Int]) extends Actor with ActorLogging {

  private val decider: Decider = {
    case StatsActor.StatstActorException =>
      log.warning("Received exception on stats, trying to resume it")
      Resume
  }

  override def supervisorStrategy: SupervisorStrategy = {
    OneForOneStrategy() {
      decider.orElse(super.supervisorStrategy.decider)
    }
  }

  private val statsActor = createStatsActor()

  override def receive: Receive = LoggingReceive {
    case message => statsActor.forward(message)
  }

  private[example] def createStatsActor(): ActorRef = {
    val acc = maybeAccuracy.getOrElse(context.system.settings.config.getInt("main.stats-accuracy"))
    context.actorOf(StatsActor.props(acc), "stats-actor")
  }

}

object StatsSupervisorActor {
  def props(maybeAccuracy: Option[Int]): Props = {
    Props(new StatsSupervisorActor(maybeAccuracy))
  }
}