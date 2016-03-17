package com.example

import akka.actor.{Actor, ActorLogging, Props}
import com.example.StatsActor.AggregateStats
import com.example.events.Request
import scala.collection.mutable.{Map => MutableMap, ArrayBuffer}
import java.time.{ZoneOffset, Instant, LocalDateTime}

/**
  * Created by denisg on 2016-03-17.
  */
class StatsActor extends Actor with ActorLogging {

  private[example] val requestsPerBrowser: MutableMap[String, Int] = MutableMap().withDefaultValue(0)
  private val requestsPerMinute: MutableMap[String, Int] = MutableMap().withDefaultValue(0)
  private val requestsPerPage: MutableMap[String, Int] = MutableMap().withDefaultValue(0)
  private val sessionDurations: ArrayBuffer[Int] = ArrayBuffer()
  private val requestsPerReferrer: MutableMap[String, Int] = MutableMap().withDefaultValue(0)


  def receive = {
    case AggregateStats(requests) =>
      processRequestsPerBrowser(requests)
      processRequestsPerMinute(requests)

  }

  private def processRequestsPerBrowser(requests: List[Request]): Unit = {
    requests.headOption.map {
      request =>
        val browser = request.session.browser
        val currentCount = requestsPerBrowser(browser)
        requestsPerBrowser(browser) = currentCount + requests.size
    }
  }

  private def processRequestsPerMinute(requests: List[Request]): Unit = {
    val timeStrings: List[String] = for {
      request <- requests
      timeStr = LocalDateTime.ofInstant(Instant.ofEpochMilli(request.timestamp), ZoneOffset.UTC).formatted("HH:mm")
    } yield timeStr

    val requestStats: Map[String, Int] = timeStrings.groupBy(identity).mapValues(_.size)
    requestStats.foreach {
      case (minute, count) =>
        val currentCount: Int = requestsPerMinute(minute)
        requestsPerMinute(minute) = currentCount + count
    }
  }

}

object StatsActor {

  case class AggregateStats(requests: List[Request])

  def props: Props = {
    Props(new StatsActor)
  }


}