/**
 * Copyright (C) 2015 Typesafe <http://typesafe.com/>
 */
package com.typesafe.sample.wordcount

import akka.actor._

class LetterActor extends Actor with ActorLogging {
  import LetterActor._

  var count = 0

  def receive = {
    case WordRootActor.CreateWord(word) ⇒
      if (word.isEmpty) count += 1
      else context.child(word.head.toString).getOrElse(context.actorOf(LetterActor.props, word.head.toString)) ! WordRootActor.CreateWord(word.tail)
    case SearchMostFrequentWords(numberWords) ⇒
      if (context.children.isEmpty) {
        sender ! WordCount(wordify(self.path), count)
      } else {
        val aggregator =
          if (count > 0) context.actorOf(SearchAggregatorActor.props(numberWords, Some(WordCount(wordify(self.path), count))))
          else context.actorOf(SearchAggregatorActor.props(numberWords))
        context.children.filterNot(_.path.toString.contains("$")) foreach { aggregator forward _ }
      }
  }

  def wordify(path: ActorPath): String = {
    val p = path.toString
    p.substring(p.indexOf(WordRootActor.name) + WordRootActor.name.length).replace("/", "")
  }
}

object LetterActor {
  def props = Props[LetterActor]

  case class SearchMostFrequentWords(numberWords: Int)
  case class WordCount(name: String, count: Int)
  case class ResultCounts(result: Vector[WordCount])
}
