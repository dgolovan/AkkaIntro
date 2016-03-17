package com.example

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.example.events.{EventProducer, Request}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class SessionActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
 
  def this() = this(ActorSystem("MySpec"))
 
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
 
  "A Session actor" must {
    "receive an event and add it to history" in {
      val request: Request = new EventProducer(1).tick.head

      val sessionActor = TestActorRef(new SessionActor(request.session))
      sessionActor ! SessionActor.AddRequest(request)
      sessionActor.underlyingActor.requestHistory.size shouldBe 1
    }
  }
}
