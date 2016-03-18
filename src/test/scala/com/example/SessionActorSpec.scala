package com.example

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import com.example.events.{EventProducer, Request}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

class SessionActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
 
  def this() = this(ActorSystem("MySpec"))
 
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
 
  "A Session actor" must {
    "receive an event and add it to history" in {
      val request: Request = new EventProducer(1).tick.head
      val timeoutDuration = 10.seconds
      val sessionActor = TestActorRef(new SessionActor(request.session, timeoutDuration, TestProbe().ref))
      sessionActor ! SessionActor.AddRequest(request)
      sessionActor.underlyingActor.requestHistory.size shouldBe 1
    }
  }

  "A Session actor" must {
    "receive an InactivityTimeout message in case it hasn't received a request in 5 min" in {
      val request: Request = new EventProducer(1).tick.head
      val timeoutDuration = 100.millis

      val sessionActor = TestActorRef(new SessionActor(request.session, timeoutDuration, TestProbe().ref))

      val probe = TestProbe()
      probe.watch(sessionActor)

      within(200.millis) {
        probe.expectTerminated(sessionActor)
      }
    }
  }

  "A Session actor" must {
    "send requestHistory to StatsActor" in {
      val request: Request = new EventProducer(1).tick.head
      val timeoutDuration = 100.millis

      val statsActorMock = TestProbe()
      system.actorOf(Props(new SessionActor(request.session, timeoutDuration, statsActorMock.ref)))

      statsActorMock.expectMsgPF(200.millis) {
        case _: StatsActor.AggregateStats => false
        case _ => throw new IllegalStateException()
      }
    }
  }

}
