package com.example

import akka.actor.{ActorRef, ActorSystem, Props}
import com.example.RequestDispatcherActor.DispatchRequests
import com.example.events.EventProducer

object ApplicationMain extends App {

  private val system = ActorSystem("MyActorSystem")
  private val requestDispatcherActor: ActorRef = system.actorOf(Props(new RequestDispatcherActor))

  private val stream = new EventProducer(5)

  (1 to 20). foreach { _ =>
    requestDispatcherActor ! DispatchRequests(stream.tick)
  }

  system.awaitTermination()
}