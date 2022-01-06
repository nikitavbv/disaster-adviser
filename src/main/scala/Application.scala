package com.nikitavbv.disaster

import akka.actor.ActorSystem
import com.nikitavbv.disaster.notifcenter.EonetNotificationCenter

import scala.concurrent.ExecutionContextExecutor

object Application {
  implicit val system: ActorSystem = ActorSystem("DisasterAdviser")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  def main(args: Array[String]) = {
    EonetNotificationCenter.monitorEvents().runForeach(println)

    println("Hello, World!")
  }
}
