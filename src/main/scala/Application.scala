package com.nikitavbv.disaster

import notifcenter.{EonetNotificationCenter, PdcNotificationCenter}
import model.DisasterFormat._

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.{Http, server}
import akka.stream.Attributes
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.nikitavbv.disaster.calendar.GoogleCalendarClient
import com.nikitavbv.disaster.model.{Disaster, Location, LocationParser}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives
import spray.json._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContextExecutor

object Application {
  implicit val system: ActorSystem = ActorSystem("DisasterAdviser")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val persistedDisasters: ListBuffer[Disaster] = ListBuffer()

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
        }
        complete("hello")
      }
    )
  }

  def persistedDisastersSource: Source[Disaster, NotUsed] = Source.fromIterator(() => persistedDisasters.iterator)

  def allDisasters: Source[Disaster, NotUsed] = persistedDisastersSource merge disasterSource

  def handleWebsocket(): Flow[Message, Message, Any] = {
    Flow[Disaster]
      .merge(allDisasters)
      .map(v => TextMessage(v.toJson.toString))
      .asInstanceOf[Flow[Message, Message, Any]]
  }

  Http().newServerAt("0.0.0.0", port).bindFlow(routes)

  def main(args: Array[String]): Unit = {
    disasterSource.runForeach(persistedDisasters.addOne)
  }
}
