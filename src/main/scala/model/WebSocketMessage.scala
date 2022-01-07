package com.nikitavbv.disaster
package model

import model.DisasterFormat._
import model.EventSafetyLevelFormat._
import model.LocationFormat._

import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import java.time.Instant

case class WebSocketMessage(action: String, token: String)

case class WebSocketMessageWithAction[T](action: String, data: T)

case class CalendarEventMessage(event: String, start: Option[Instant], safetyLevel: EventSafetyLevel.Value)

object WebSocketMessageFormat {
  implicit val messageFormat: RootJsonFormat[WebSocketMessage] = jsonFormat2(WebSocketMessage)
  implicit val newDisasterMessageFormat: RootJsonFormat[WebSocketMessageWithAction[Disaster]] = jsonFormat2(WebSocketMessageWithAction[Disaster])
  implicit val hotPointsMessageFormat: RootJsonFormat[WebSocketMessageWithAction[Seq[(Location, Int)]]] = jsonFormat2(WebSocketMessageWithAction[Seq[(Location, Int)]])
  implicit val calendarEventMessageFormat: RootJsonFormat[CalendarEventMessage] = jsonFormat3(CalendarEventMessage)
  implicit val calendarEventWebsocketMessageFormat: RootJsonFormat[WebSocketMessageWithAction[CalendarEventMessage]] = jsonFormat2(WebSocketMessageWithAction[CalendarEventMessage])
}