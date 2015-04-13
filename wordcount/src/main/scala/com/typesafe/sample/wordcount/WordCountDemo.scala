/**
 * Copyright (C) 2015 Typesafe <http://typesafe.com/>
 */
package com.typesafe.sample.wordcount

import akka.actor.ActorDSL._
import akka.actor._
import scala.io.Source

object WordCountDemo {
  def main(args: Array[String]) {
    if (args.size != 3) {
      println("To execute you need to pass these three arguments: <fileName> <numberOfSearchesToPerform> <numberOfWordsToFind>")
    } else {
      val system = ActorSystem("WordCountDemo")
      val fileName = args(0)
      val numberOfSearchesToPerform = args(1).toInt
      val numberOfWordsToFind = args(2).toInt
      val main = system.actorOf(MainActor.props, "main")
      main ! MainActor.StartMeasurement
      main ! MainActor.CreateFromFile(fileName)
      1 to numberOfSearchesToPerform foreach { p ⇒ main ! LetterActor.SearchMostFrequentWords(numberOfWordsToFind) }
    }
  }
}
