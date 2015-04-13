/**
 * Copyright (C) 2015 Typesafe <http://typesafe.com/>
 */
package com.typesafe.sample.wordcount

import akka.actor.{ Props, Actor, ActorLogging }

class WordRootActor extends Actor with ActorLogging {
  import WordRootActor._

  def receive = {
    case CreateWord(word) ⇒
      context.child(word.head.toString).getOrElse(context.actorOf(LetterActor.props, word.head.toString)) ! CreateWord(word.tail)
    case LetterActor.SearchMostFrequentWords(numberWords) ⇒
      val aggregator = context.actorOf(SearchAggregatorActor.props(numberWords))
      context.children.filterNot(_.path.toString.contains("$")).foreach(aggregator forward _)
  }
}

object WordRootActor {
  val name = "root"

  def props = Props[WordRootActor]

  case class CreateWord(word: String)
}

