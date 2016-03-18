package com.example

import akka.actor.{ActorRef, ActorSystem}
import com.example.RequestDispatcherActor.DispatchRequests
import com.example.events.EventProducer

import scala.util.Random

object ApplicationMain extends App {

  private val system = ActorSystem("MyActorSystem")
  private val statsActor: ActorRef = system.actorOf(StatsSupervisorActor.props(None), "stats-supervisor")
  private val requestDispatcherActor: ActorRef = system.actorOf(RequestDispatcherActor.props(statsActor), "dispatcher")
  private val stream = new EventProducer(3)


  while(true) {
    println("Sending ticks")
    (1 to Random.nextInt(5)).foreach(_ => requestDispatcherActor ! DispatchRequests(stream.tick))

    val time = Random.nextInt(7) * 1000
    println(s"Sleeping $time millis")
    Thread.sleep(time)
  }

  system.awaitTermination()
}