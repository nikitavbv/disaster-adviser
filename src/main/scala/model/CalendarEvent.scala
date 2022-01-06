package com.nikitavbv.disaster
package model

import com.google.api.services.calendar.model.Event

case class CalendarEvent(title: String, location: String)

object CalendarEvent {

  def from(event: Event): CalendarEvent = CalendarEvent(
    event.getSummary,
    event.getLocation
  )
}