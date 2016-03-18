package com.example

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneOffset}

import akka.actor.{Actor, ActorLogging, Props}
import com.example.StatsActor.AggregateStats
import com.example.events.Request

import scala.collection.mutable.{ArrayBuffer, Map => MutableMap}

/**
  * Created by denisg on 2016-03-17.
  */
class StatsActor extends Actor with ActorLogging {

  private[example] val requestsPerBrowser: MutableMap[String, Int] = MutableMap().withDefaultValue(0)
  private[example] val requestsPerMinute: MutableMap[String, Int] = MutableMap().withDefaultValue(0)
  private[example] val requestsPerPage: MutableMap[String, Int] = MutableMap().withDefaultValue(0)
  private val sessionDurations: ArrayBuffer[Int] = ArrayBuffer()
  private val requestsPerReferrer: MutableMap[String, Int] = MutableMap().withDefaultValue(0)


  def receive = {
    case AggregateStats(requests) =>
      processRequestsPerBrowser(requests)
      processRequestsPerMinute(requests)
      processRequestsPerPage(requests)
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
      timeStr = LocalDateTime.ofInstant(Instant.ofEpochMilli(request.timestamp), ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("HH:mm"))
    } yield timeStr

    val requestStats: Map[String, Int] = timeStrings.groupBy(identity).mapValues(_.size)
    requestStats.foreach {
      case (minute, count) =>
        val currentCount: Int = requestsPerMinute(minute)
        requestsPerMinute(minute) = currentCount + count
    }
  }

  private def processRequestsPerPage(requests: List[Request]): Unit = {
    val urlsStats: Map[String, Int] =  requests.map(r => r.url).groupBy(identity).mapValues(_.size)

    urlsStats.foreach {
      case (url, count) =>
        val currentCount: Int = requestsPerPage(url)
        requestsPerPage(url) = currentCount + count
    }
  }

}

object StatsActor {

  case class AggregateStats(requests: List[Request])

  def props: Props = {
    Props(new StatsActor)
  }


}