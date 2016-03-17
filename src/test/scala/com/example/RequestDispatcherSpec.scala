package com.example

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.example.SessionActor.AddRequest
import com.example.events.{EventProducer, Request, Session}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class RequestDispatcherSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("MySpec"))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A RequestDispatcher actor" must {
    "receive a list of requests and send it to SessionActor" in {
      val requests: List[Request] = new EventProducer(1).tick
      val sessionActorMock = TestProbe()
      val dispatcherActor = system.actorOf(Props(new MockDispatcherActor(sessionActorMock.ref)))

      dispatcherActor ! RequestDispatcherActor.DispatchRequests(requests)
      sessionActorMock.expectMsgPF() {
        case _: AddRequest => Unit
        case _ => throw new IllegalStateException()
      }
    }
  }

  /**
    * Mock overrides creating real session actor with TestProbe
    */
  class MockDispatcherActor(sessionActor: ActorRef) extends RequestDispatcherActor {

    override def createSessionActor(session: Session): ActorRef = {
      sessionActor
    }
  }

}
