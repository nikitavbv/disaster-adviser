package com.nikitavbv.disaster
package model

import DisasterFormat._
import LocationFormat._

import spray.json.{JsValue, RootJsonFormat, jsonReader, jsonWriter}
import spray.json.DefaultJsonProtocol._

case class WebSocketMessage(action: String, token: String)

case class WebSocketMessageWithAction[T](action: String, data: T)

object WebSocketMessageFormat {
  implicit val messageFormat: RootJsonFormat[WebSocketMessage] = jsonFormat2(WebSocketMessage)
  implicit val newDisasterMessageFormat: RootJsonFormat[WebSocketMessageWithAction[Disaster]] = jsonFormat2(WebSocketMessageWithAction[Disaster])
  implicit val hotPointsMessageFormat: RootJsonFormat[WebSocketMessageWithAction[Seq[(Location, Int)]]] = jsonFormat2(WebSocketMessageWithAction[Seq[(Location, Int)]])
}