package com.nikitavbv.disaster
package model

import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

case class Disaster(id: String,
                    title: String,
                    location: Seq[DisasterLocation])

case class DisasterLocation(latitude: Double,
                            longitude: Double)

object DisasterFormat {

  implicit val locationFormat: RootJsonFormat[DisasterLocation] = jsonFormat2(DisasterLocation)
  implicit val disasterFormat: RootJsonFormat[Disaster] = jsonFormat3(Disaster)
}