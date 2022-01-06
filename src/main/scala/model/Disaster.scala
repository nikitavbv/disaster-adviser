package com.nikitavbv.disaster
package model

import LocationFormat._

import spray.json.DefaultJsonProtocol._
import spray.json.{JsString, JsValue, RootJsonFormat}

import java.time.Instant

case class Disaster(id: String,
                    title: String,
                    location: Seq[Location],
                    startDate: Option[Instant],
                    endDate: Option[Instant]) {

  def isCloseTo(location: Location): Boolean = this.location.exists(_.isCloseTo(location))
}

object DisasterFormat {
  implicit val disasterFormat: RootJsonFormat[Disaster] = jsonFormat5(Disaster)

  implicit object InstantWriter extends RootJsonFormat[Instant] {
    override def write(obj: Instant): JsValue = JsString(obj.toString)
    override def read(json: JsValue): Instant = Instant.parse(json.toString)
  }
}