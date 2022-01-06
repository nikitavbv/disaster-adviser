package com.nikitavbv.disaster
package model

import com.google.api.services.calendar.model.Event

import java.time.Instant

case class CalendarEvent(title: String, location: String, start: Option[Instant], end: Option[Instant])

object CalendarEvent {

  def from(event: Event): CalendarEvent = CalendarEvent(
    event.getSummary,
    event.getLocation,
    Option(event.getStart).flatMap(v => Option(v.getDateTime)).map(_.getValue).map(Instant.ofEpochMilli),
    Option(event.getEnd).flatMap(v => Option(v.getDateTime)).map(_.getValue).map(Instant.ofEpochMilli)
  )
}