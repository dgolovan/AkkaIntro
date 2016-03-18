package com.example

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneOffset}

import akka.actor.ActorSystem
import akka.actor.SupervisorStrategy.{Restart, Resume}
import akka.testkit.{EventFilter, ImplicitSender, TestActorRef, TestKit}
import com.example.events.{EventProducer, Request}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._


//class StatsActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
//with WordSpecLike with Matchers with BeforeAndAfterAll {

class StatsActorSpec(_system: ActorSystem) extends TestKit(ActorSystem("MyTest", ConfigFactory.parseString(
  """
    |akka {
    |  loggers = ["akka.event.slf4j.Slf4jLogger", "akka.testkit.TestEventListener"]
    |  loglevel = debug
    |}
    |
    |main {
    |  session-timeout = 5000
    |  stats-accuracy = 100
    |}
    |
  """.stripMargin
))) with ImplicitSender
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

      val statsActor = TestActorRef(new StatsActor(100))
      statsActor ! StatsActor.AggregateStats(requests)

      statsActor.underlyingActor.requestsPerBrowser shouldEqual (Map(browser -> requestNum))
    }
  }

  "A Stats actor" must {
    "receive a list of events from SessionActor and calculate requests/minute" in {
      val requestNum = 5
      val requests: List[Request] = new EventProducer(requestNum).tick

      val timestamp = requests.head.timestamp
      val timeStr = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("HH:mm"))
      val statsActor = TestActorRef(new StatsActor(100))
      statsActor ! StatsActor.AggregateStats(requests)

      statsActor.underlyingActor.requestsPerMinute shouldEqual (Map(timeStr -> requestNum))
    }


    "resume or restart child depending on actor strategy" in {
      val requestNum = 5
      val requests: List[Request] = new EventProducer(requestNum).tick
      case object StatstActorException2 extends IllegalStateException

      val statsSupervisorActor = TestActorRef(new StatsSupervisorActor(Some(100)))
      val strategy = statsSupervisorActor.underlyingActor.supervisorStrategy.decider
      strategy(StatsActor.StatstActorException) shouldBe Resume
      strategy(StatstActorException2) shouldBe Restart
    }

    "throw an exception if accuracy is 0" in {
      val requestNum = 5
      val requests: List[Request] = new EventProducer(requestNum).tick
      case object StatstActorException2 extends IllegalStateException

      val statsSupervisorActor = TestActorRef(new StatsSupervisorActor(Some(0)))

      EventFilter.warning(message = "Received exception on stats, trying to resume it", occurrences = 1).intercept {
        statsSupervisorActor ! StatsActor.AggregateStats(requests)
      }
    }
  }
}