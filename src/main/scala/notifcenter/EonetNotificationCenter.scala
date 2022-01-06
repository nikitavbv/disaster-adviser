package com.nikitavbv.disaster
package notifcenter

import Application._

import akka.NotUsed
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.scaladsl.Source
import spray.json.RootJsonFormat

import scala.collection.mutable
import scala.concurrent.duration.DurationInt

case class EonetResponse(title: String, events: Seq[EonetEvent])

case class EonetEvent(id: String, title: String, geometry: Seq[EonetEventGeometry])

case class EonetEventGeometry(coordinates: (Double, Double))

object EonetNotificationCenter {
  implicit val eventGeometryFormat: RootJsonFormat[EonetEventGeometry] = jsonFormat1(EonetEventGeometry)
  implicit val eventFormat: RootJsonFormat[EonetEvent] = jsonFormat3(EonetEvent)
  implicit val responseFormat: RootJsonFormat[EonetResponse] = jsonFormat2(EonetResponse)

  def monitorEvents(): Source[EonetEvent, NotUsed] = {
    val processedEvents: mutable.HashSet[String] = mutable.HashSet()

    Source.tick(
      1.second,
      10.seconds,
      ()
    )
      .flatMapConcat(_ => loadEvents())
      .filter(event => !processedEvents.contains(event.id))
      .wireTap(event => processedEvents.add(event.id))
      .mapMaterializedValue(_ => NotUsed.notUsed())
  }

  def loadEvents(): Source[EonetEvent, NotUsed] = {
    Source.future(Http()
      .singleRequest(
        HttpRequest(GET, uri = "https://eonet.gsfc.nasa.gov/api/v3/events")
          .withHeaders(RawHeader("Accept", "application/json"))
      )
      .flatMap(v => Unmarshal(v.entity).to[EonetResponse]))
      .flatMapConcat(res => Source(res.events))
  }
}