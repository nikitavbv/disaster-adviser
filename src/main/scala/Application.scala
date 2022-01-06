package com.nikitavbv.disaster

import akka.actor.ActorSystem
import com.nikitavbv.disaster.notifcenter.{EonetNotificationCenter, PdcNotificationCenter}

import scala.concurrent.ExecutionContextExecutor

object Application {
  implicit val system: ActorSystem = ActorSystem("DisasterAdviser")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  def main(args: Array[String]) = {
    Seq(
      EonetNotificationCenter.monitorEvents().map(_.toDisaster),
      PdcNotificationCenter.monitorEvents().map(_.toDisaster)
    ).reduce((a, b) => a.merge(b)).runForeach(println)

    println("Hello, World!")
  }
}
