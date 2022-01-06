package com.nikitavbv.disaster

import notifcenter.{EonetNotificationCenter, PdcNotificationCenter}
import model.DisasterFormat._

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives.{concat, get, getFromResource, getFromResourceDirectory, handleWebSocketMessages, path, pathSingleSlash}
import akka.http.scaladsl.{Http, server}
import akka.stream.Attributes
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.nikitavbv.disaster.calendar.GoogleCalendarClient
import com.nikitavbv.disaster.model.{Disaster, DisasterLocation}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

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
    .alsoTo(Sink.foreach(persistedDisasters.addOne))

  val port = 8080

  val routes: server.Route = get {
    concat(
      path("disasters") {
        handleWebSocketMessages(handleWebsocket())
      }
    )
  }

  def handleWebsocket(): Flow[Message, Message, Any] = {
    Flow[Disaster]
      .merge(Source.fromIterator(() => persistedDisasters.iterator).merge(disasterSource))
      .map(v => TextMessage(v.toJson.toString))
      .asInstanceOf[Flow[Message, Message, Any]]
  }

  Http().newServerAt("0.0.0.0", port).bindFlow(routes)

  def main(args: Array[String]) = {
    /*new GoogleCalendarClient(sys.env("GOOGLE_CALENDAR_TOKEN"))
      .allEvents
      .runForeach(println)
      .onComplete(println)*/

    println(s"server started at port ${port}")
    ()
  }
}
