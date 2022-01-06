package com.nikitavbv.disaster
package notifcenter

import Application._
import model.{Disaster, Location}

import akka.NotUsed
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport.defaultNodeSeqUnmarshaller
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.HttpRequest
import akka.stream.scaladsl.Source

import java.time.Instant
import scala.collection.mutable
import scala.concurrent.duration.DurationInt
import scala.xml.NodeSeq

case class PdcResponse(hazardBeans: Seq[PdcHazardBean])
case class PdcHazardBean(hazardId: String,
                         hazardName: String,
                         latitude: Double,
                         longitude: Double,
                         startDate: String,
                         endDate: String) {

  def toDisaster: Disaster = Disaster(
    s"PDC-${this.hazardId}",
    this.hazardName,
    Seq(Location(this.latitude, this.longitude)),
    Some(Instant.parse(startDate)),
    Some(Instant.parse(endDate))
  )
}

object PdcNotificationCenter {

  def monitorEvents(): Source[PdcHazardBean, NotUsed] = {
    val processedEvents: mutable.HashSet[String] = mutable.HashSet()

    Source.tick(
      1.second,
      60.seconds,
      ()
    )
      .flatMapConcat(_ => loadEvents())
      .filter(event => !processedEvents.contains(event.hazardId))
      .wireTap(event => processedEvents.add(event.hazardId))
      .mapMaterializedValue(_ => NotUsed.notUsed())
  }

  def loadEvents(): Source[PdcHazardBean, NotUsed] = {
    Source.future(Http()
      .singleRequest(
        HttpRequest(GET, uri=  "https://hpxml.pdc.org/public.xml")
      )
      .flatMap(v => defaultNodeSeqUnmarshaller(v.entity)))
      .map(PdcResponse.fromXml)
      .flatMapConcat(res => Source(res.hazardBeans))
  }
}

object PdcResponse {
  def fromXml(xml: NodeSeq): PdcResponse = {
    PdcResponse((xml \ "hazardBean").map(PdcHazardBean.fromXml))
  }
}

object PdcHazardBean {
  def fromXml(xml: NodeSeq): PdcHazardBean = {
    PdcHazardBean(
      (xml \ "hazard_ID").text,
      (xml \ "hazard_Name").text,
      (xml \ "latitude").text.toDouble,
      (xml \ "longitude").text.toDouble,
      (xml \ "start_Date").text,
      (xml \ "end_Date").text
    )
  }
}