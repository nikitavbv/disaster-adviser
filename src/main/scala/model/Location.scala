package com.nikitavbv.disaster
package model

import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

case class Location(latitude: Double,
                    longitude: Double) {

  def distanceTo(other: Location): Double = {
    val AVERAGE_RADIUS_OF_EARTH_KM = 6371

    val latDistance = Math.toRadians(this.latitude - other.latitude)
    val lngDistance = Math.toRadians(this.longitude - other.longitude)
    val sinLat = Math.sin(latDistance / 2)
    val sinLng = Math.sin(lngDistance / 2)
    val a = sinLat * sinLat +
      (Math.cos(Math.toRadians(this.latitude)) *
        Math.cos(Math.toRadians(other.longitude)) *
        sinLng * sinLng)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    AVERAGE_RADIUS_OF_EARTH_KM * c
  }

  def isCloseTo(other: Location): Boolean = distanceTo(other) < 250.0 // TODO: implement this
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