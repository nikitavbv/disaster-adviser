package com.nikitavbv.disaster

import notifcenter.{EonetNotificationCenter, PdcNotificationCenter}

import akka.actor.ActorSystem
import com.nikitavbv.disaster.calendar.GoogleCalendarClient

import scala.concurrent.ExecutionContextExecutor

object Application {
  implicit val system: ActorSystem = ActorSystem("DisasterAdviser")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  def main(args: Array[String]) = {
   /*Seq(
      EonetNotificationCenter.monitorEvents().map(_.toDisaster),
      PdcNotificationCenter.monitorEvents().map(_.toDisaster)
    ).reduce((a, b) => a merge b).runForeach(println)*/

    new GoogleCalendarClient(sys.env("GOOGLE_CALENDAR_TOKEN"))
      .allEvents
      .runForeach(println)
      .onComplete(println)

    println("Hello, World!")
  }
}
