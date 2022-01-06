package com.nikitavbv.disaster
package model

import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

case class Location(latitude: Double,
                    longitude: Double) {

  def isCloseTo(other: Location): Boolean = true // TODO: implement this
}


object LocationParser {

  def fromText(text: String): Option[Location] = {
    if (!text.contains(",")) {
      return None
    }

    val spl = text.split(",")
    val latitude = spl(0).toDoubleOption
    if (latitude.isEmpty) {
      return None
    }

    val longitude = spl(1).toDoubleOption
    if (longitude.isEmpty) {
      return None
    }

    Some(Location(latitude.get, longitude.get))
  }
}

object LocationFormat {
  implicit val locationFormat: RootJsonFormat[Location] = jsonFormat2(Location)
}