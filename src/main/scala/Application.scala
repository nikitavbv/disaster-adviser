package com.nikitavbv.disaster

import calendar.GoogleCalendarClient
import model.WebSocketMessageFormat._
import model._
import notifcenter.{EonetNotificationCenter, PdcNotificationCenter}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.{Http, server}
import akka.stream.scaladsl.{Flow, Sink, Source}
import spray.json._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt

object Application {
  implicit val system: ActorSystem = ActorSystem("DisasterAdviser")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val persistedDisasters: ListBuffer[Disaster] = ListBuffer()
  val hotPoints: mutable.HashMap[String, Int] = mutable.HashMap()

  var disasterSource: Source[Disaster, NotUsed] = Seq(
    EonetNotificationCenter.monitorEvents().map(_.toDisaster),
    PdcNotificationCenter.monitorEvents().map(_.toDisaster)
  ).reduce((a, b) => a merge b)

  def persistedDisastersSource: Source[Disaster, NotUsed] = Source.fromIterator(() => persistedDisasters.iterator)

  def allDisasters: Source[Disaster, NotUsed] = persistedDisastersSource merge disasterSource

  def hotPointsCenters: Source[Location, NotUsed] = Source(hotPoints.toSeq
      .sortBy(k => k._2)
      .reverse
      .slice(0, 10)
  ).map(point => persistedDisasters
    .filter(disaster => disaster.locationCluster == point._1)
    .map(_.center)
    .reduce((a, b) => a centerBetween b))

  val port = 8080

  val routes: server.Route = get {
    concat(
      path("ws") {
        handleWebSocketMessages(handleWebsocket())
      }
    )
  }

  def handleWebsocket(): Flow[Message, Message, Any] = {
    val hotPointsSource = Source.tick(
      10.seconds,
      60.seconds,
      ()
    )
      .map(_ => {
        hotPoints.toSeq
          .sortBy(k => k._2)
          .reverse
          .slice(0, 10)
          .map(point => (persistedDisasters
            .filter(disaster => disaster.locationCluster == point._1)
            .map(_.center)
            .reduce((a, b) => a centerBetween b), point._2))
      })

    Flow[Message]
      .map(_.asTextMessage.getStrictText)
      .flatMapConcat(message => Source.future(Unmarshal(message).to[WebSocketMessage]))
      .flatMapConcat(message => {
        new GoogleCalendarClient(message.token)
          .allEvents
          .filter(_.isUpcoming)
          .flatMapConcat(event => {
            val location = LocationParser.fromText(event.location)
            if (location.isEmpty) {
              Source.empty
            } else {
              Source.future(persistedDisastersSource
                .filter(disaster => disaster isCloseTo location.get)
                .map(_ => EventSafetyLevel.WithinDisaster)
                .orElse(Source.single(EventSafetyLevel.Ok))
                .orElse(hotPointsCenters.filter(center => center isCloseTo location.get)
                    .map(_ => EventSafetyLevel.WithinHotPoint))
                .runWith(Sink.head))
                .map(status => CalendarEventMessage(event.title, event.start, status))
                .map(msg => TextMessage(WebSocketMessageWithAction("calendar_event", msg).toJson.toString))
            }
          })
      })
      .merge(
        allDisasters.map(v => TextMessage(WebSocketMessageWithAction("new_disaster", v).toJson.toString))
      )
      .merge(
        hotPointsSource.map(v => TextMessage(WebSocketMessageWithAction("hot_points", v).toJson.toString))
      )
  }

  Http().newServerAt("0.0.0.0", port).bindFlow(routes)

  def main(args: Array[String]): Unit = {
    disasterSource
      .alsoTo(Sink.foreach(disaster => hotPoints.put(disaster.locationCluster, hotPoints.getOrElse(disaster.locationCluster, 0) + 1)))
      .runForeach(persistedDisasters.addOne)
  }
}
