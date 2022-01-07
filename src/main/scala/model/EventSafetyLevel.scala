package com.nikitavbv.disaster
package model

import spray.json.{JsString, JsValue, RootJsonFormat}

object EventSafetyLevel extends Enumeration {
  val Ok, WithinDisaster, WithinHotPoint = Value
}

object EventSafetyLevelFormat {

  implicit object EventSafetyLevelWriter extends RootJsonFormat[EventSafetyLevel.Value] {
    override def write(obj: EventSafetyLevel.Value): JsValue = JsString(obj.toString)
    override def read(json: JsValue): EventSafetyLevel.Value = EventSafetyLevel.withName(json.toString)
  }
}