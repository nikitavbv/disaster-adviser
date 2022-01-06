package com.nikitavbv.disaster

import akka.actor.ActorSystem
import com.nikitavbv.disaster.notifcenter.{EonetNotificationCenter, PdcNotificationCenter}

import scala.concurrent.ExecutionContextExecutor

object Application {
  implicit val system: ActorSystem = ActorSystem("DisasterAdviser")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  def main(args: Array[String]) = {
    // EonetNotificationCenter.monitorEvents().runForeach(println)
    PdcNotificationCenter.monitorEvents().runForeach(println)

    println("Hello, World!")
  }
}
