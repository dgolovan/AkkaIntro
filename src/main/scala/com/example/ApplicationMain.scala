package com.example

import akka.actor.{ActorRef, ActorSystem}
import com.example.RequestDispatcherActor.DispatchRequests
import com.example.events.EventProducer

object ApplicationMain extends App {

  private val system = ActorSystem("MyActorSystem")
  private val statsActor: ActorRef = system.actorOf(StatsActor.props)
  private val requestDispatcherActor: ActorRef = system.actorOf(RequestDispatcherActor.props(statsActor))
  private val stream = new EventProducer(5)

  (1 to 20). foreach { _ =>
    requestDispatcherActor ! DispatchRequests(stream.tick)
  }

  system.awaitTermination()
}