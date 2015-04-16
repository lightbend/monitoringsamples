/**
 * Copyright (C) 2015 Typesafe <http://typesafe.com/>
 */
package com.typesafe.sample.wordcount

import akka.actor._

class SearchAggregatorActor(numberWords: Int, wordCount: Option[LetterActor.WordCount]) extends Actor with ActorLogging {
  var caller: Option[ActorRef] = None
  var outstandingResults = 0
  var current = Vector.empty[LetterActor.WordCount]
  if (wordCount.isDefined) updateResult(wordCount.get)

  def receive = {
    case letter: ActorRef ⇒
      caller = Some(sender)
      outstandingResults += 1
      letter ! LetterActor.SearchMostFrequentWords(numberWords)
    case wc @ LetterActor.WordCount(_, _) ⇒
      outstandingResults -= 1
      updateResult(wc)
      checkStatus()
    case LetterActor.ResultCounts(wordCounts) ⇒
      outstandingResults -= 1
      wordCounts.foreach(updateResult(_))
      checkStatus()
  }

  def updateResult(wc: LetterActor.WordCount) {
    if (current.size < numberWords) {
      current = LetterActor.WordCount(wc.name, wc.count) +: current
    } else {
      current.find(_.count < wc.count) match {
        case Some(_) ⇒ current = wc +: current.sortBy(_.count).tail
        case None    ⇒ // all words have larger count than this word -> do nothing
      }
    }
  }

  def checkStatus() {
    if (outstandingResults == 0 && caller.isDefined) {
      caller.get ! LetterActor.ResultCounts(current)
      self ! PoisonPill
    }
  }
}

object SearchAggregatorActor {
  def props(numberWords: Int, wordCount: Option[LetterActor.WordCount] = None) = Props(new SearchAggregatorActor(numberWords, wordCount))
}
