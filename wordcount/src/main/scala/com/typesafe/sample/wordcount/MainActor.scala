/**
 * Copyright (C) 2015 Typesafe <http://typesafe.com/>
 */
package com.typesafe.sample.wordcount

import akka.actor._
import scala.io.{BufferedSource, Source}
import java.io.FileNotFoundException

class MainActor extends Actor {
  import MainActor._

  val root = context.actorOf(Props[WordRootActor], WordRootActor.name)

  // TODO : currently the execution time also contains "println" and that's not optimal...
  var startTime: Long = 0
  var outstanding = 0
  var result = Vector.empty[String]

  def receive = {
    case StartMeasurement ⇒
      startTime = System.currentTimeMillis

    case StopMeasurement ⇒
      val stopTime = System.currentTimeMillis
      println("Result:")
      result foreach { println(_) }
      println("Total execution time: "  + (stopTime - startTime) + "ms")
      context.system.shutdown()

    case CreateFromFile(name) ⇒
      var source: Option[BufferedSource] = None
      try {
        source = Some(Source.fromFile(name))
        source.map{ s =>
          s.getLines().foreach { l =>
            l.split(" ").map(_.replaceAll("[,.]", "")).filter(_.matches("(\\w)+")).foreach(w ⇒ root ! WordRootActor.CreateWord(w.toLowerCase))
          }
        }
      } catch {
        case f: FileNotFoundException => 
          println(s"Could not find file: $name - exiting")
          context.system.shutdown()
      } finally {
        source.map { _.close() }
      }

    case LetterActor.SearchMostFrequentWords(numberWords) ⇒
      outstanding += 1
      root ! LetterActor.SearchMostFrequentWords(numberWords)

    case LetterActor.ResultCounts(results) ⇒
      outstanding -= 1
      results.sortBy(_.count).reverse.foreach { r ⇒ result = result :+ "[Count, Word]: " + r.count + " : " + r.name }
      result = result :+ "-----------------------------------------"
      if (outstanding == 0) self ! StopMeasurement
  }
}

object MainActor {
  def props = Props[MainActor]

  case class CreateFromFile(fileName: String)
  case object StartMeasurement
  case object StopMeasurement
}
