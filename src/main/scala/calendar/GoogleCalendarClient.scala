package com.nikitavbv.disaster
package calendar

import Application._

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.auth.oauth2.Credential.AccessMethod
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.services.calendar.Calendar
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.calendar.model.Event
import com.nikitavbv.disaster.model.CalendarEvent

import scala.jdk.CollectionConverters.CollectionHasAsScala

class GoogleCalendarClient(val accessToken: String) {

  private val client = new Calendar.Builder(
    GoogleNetHttpTransport.newTrustedTransport(),
    JacksonFactory.getDefaultInstance,
    new GoogleCredential().setAccessToken(accessToken)
  ).build()

  def allEvents: Source[CalendarEvent, NotUsed] = {
    calendars.map(_.getId).flatMapConcat(events)
  }

  def events(calendarId: String) = {
    Source.fromIterator(() => client.events().list(calendarId).execute().getItems.asScala.iterator)
      .map(CalendarEvent.from)
  }

  def calendars = {
    Source.fromIterator(() => client.calendarList().list().execute().getItems.asScala.iterator)
  }
}
