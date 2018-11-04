package com.github.propi.wordsfetcher

/**
  * Created by propan on 22. 4. 2017.
  */
object FrequentPhrases {

  /*case class Phrase(words: Seq[WordsDb.Word], support: Double)

  def apply(pattern: String, limit: Int): List[Phrase] = {
    val words = pattern.split(' ').toList
    val queue = collection.mutable.PriorityQueue.empty[Phrase](Ordering.by[Phrase, Double](_.support).reverse)
    def addToQueue(words: List[String], variables: Map[Int, Char], phrase: Phrase): Unit = words match {
      case head :: tail =>
        val words = WordsDb.searchByPattern(variables.foldLeft(head)((x, y) => x.replace("?" + y._1, "" + y._2)))
        val maxFrequency = if (words.nonEmpty) words.iterator.map(_.frequency).max else 0
        words.foreach { word =>
          addToQueue(tail, word.variables ++ variables, Phrase(phrase.words :+ word, phrase.support + (word.frequency.toDouble / maxFrequency)))
        }
      case _ => if (queue.size < limit) {
        queue.enqueue(phrase)
      } else if (phrase.support > queue.head.support) {
        queue.dequeue()
        queue.enqueue(phrase)
      }
    }
    addToQueue(words, Map.empty, Phrase(Vector.empty, 0))
    queue.dequeueAll.reverseIterator.map(x => x.copy(support = x.support / x.words.length)).toList
  }*/


}
