package com.nikitavbv.disaster

import notifcenter.{EonetNotificationCenter, PdcNotificationCenter}
import model.DisasterFormat._
import model.WebSocketMessageFormat._

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.{Http, server}
import akka.stream.Attributes
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.nikitavbv.disaster.calendar.GoogleCalendarClient
import com.nikitavbv.disaster.model.{Disaster, Location, LocationParser, WebSocketMessage, WebSocketMessageWithAction}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.unmarshalling.Unmarshal
import spray.json.DefaultJsonProtocol._
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

  val port = 8080

  val routes: server.Route = get {
    concat(
      path("ws") {
        handleWebSocketMessages(handleWebsocket())
      },
      get {
        try {
          persistedDisastersSource
            .groupBy(10000, _.locationCluster)
            .map(disaster => (disaster.locationCluster, 1))
            .reduce((a, b) => (a._1, a._2 + b._2))
            .mergeSubstreams
            .runForeach(println)
            .onComplete(_ => println("________________________-"))

          /*allDisasters
            .groupBy(Int.MaxValue, _.locationCluster)
            .map(disaster => (disaster.locationCluster, 1))
            .reduce((a, b) => (a._1, a._2 + b._2))
            .mergeSubstreams
            .runForeach(println)
            .onComplete(println)*/
        } catch {
          case e: Exception => println(e)
        }

        /*try {
          new GoogleCalendarClient(sys.env("GOOGLE_CALENDAR_TOKEN"))
            .allEvents
            .filter(_.isUpcoming)
            .flatMapConcat(event => {
              val location = LocationParser.fromText(event.location)
              if (location.isEmpty) {
                Source.empty
              } else {
                persistedDisastersSource
                  .filter(disaster => disaster isCloseTo location.get)
                  .zipWith(Source.repeat(event)) { (a, b) => (a, b) }
              }
            })
            .runForeach(println)
            .onComplete(println)
        } catch {
          case e: Exception => println(e)
        }*/

        complete("hello")
      }
    )
  }

  def persistedDisastersSource: Source[Disaster, NotUsed] = Source.fromIterator(() => persistedDisasters.iterator)

  def allDisasters: Source[Disaster, NotUsed] = persistedDisastersSource merge disasterSource

  def handleWebsocket(): Flow[Message, Message, Any] = {
    var googleCalendarClient: Option[GoogleCalendarClient] = None
    val hotPointsSource = Source.tick(
      10.seconds,
      60.seconds,
      ()
    )
      .map(_ => {
        hotPoints.toSeq
          .sortBy(k => k._2)
          .reverse
          .slice(0, 5)
          .map(point => (persistedDisasters
            .filter(disaster => disaster.locationCluster == point._1)
            .map(_.center)
            .reduce((a, b) => a centerBetween b), point._2))
      })

    Flow[Message]
      .map(_.asTextMessage.getStrictText)
      .flatMapConcat(message => Source.future(Unmarshal(message).to[WebSocketMessage]))
      .wireTap(message => {
        googleCalendarClient = Some(new GoogleCalendarClient(message.token))
        // TODO: finish this
      })
      .flatMapConcat(_ => Source.empty[TextMessage])
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
