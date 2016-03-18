package com.example

import java.time.format.DateTimeFormatter
import java.time.{ZoneOffset, Instant, LocalDateTime}

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.example.events.{EventProducer, Request}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import scala.concurrent.duration._


class StatsActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("MySpec2"))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A Stats actor" must {
    "receive a list of events from SessionActor and calculate requests/browser" in {
      val requestNum = 3
      val requests: List[Request] = new EventProducer(requestNum).tick
      val browser = requests.head.session.browser
      val timeoutDuration = 10.seconds

      val statsActor = TestActorRef(new StatsActor)
      statsActor ! StatsActor.AggregateStats(requests)

      statsActor.underlyingActor.requestsPerBrowser shouldEqual(Map(browser -> requestNum))
    }
  }

  "A Stats actor" must {
    "receive a list of events from SessionActor and calculate requests/minute" in {
      val requestNum = 5
      val requests: List[Request] = new EventProducer(requestNum).tick

      val timestamp = requests.head.timestamp
      val timeStr = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("HH:mm"))
      val statsActor = TestActorRef(new StatsActor)
      statsActor ! StatsActor.AggregateStats(requests)

      statsActor.underlyingActor.requestsPerMinute shouldEqual(Map(timeStr -> requestNum))
    }
  }
}