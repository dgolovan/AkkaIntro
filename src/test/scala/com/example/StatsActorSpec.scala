package com.example

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.example.events.{EventProducer, Request}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

class StatsActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("MySpec"))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A Stats actor" must {
    "receive a list of events from SessionActor and calculate requests/browser" in {

      val requests: List[Request] = new EventProducer(1).tick
      val timeoutDuration = 10.seconds

      val statsActor = TestActorRef(StatsActor.props)
      statsActor ! StatsActor.AggregateStats(requests)

      statsActor.underlyingActor
    }
  }
}