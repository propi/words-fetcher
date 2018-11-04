package com.github.propi.wordsfetcher

import java.io.{File, PrintWriter}
import java.text.Normalizer

/**
  * Created by propan on 21. 4. 2017.
  */
class WordsDbWriter private(directory: File) {

  private val fileMap = collection.mutable.Map.empty[String, PrintWriter]

  private def wordToFileName(word: String) = word.length + "-" + word.head

  def save(text: String): Unit = {
    Normalizer
      .normalize(
        text.replaceAll("\\s+", " ")
          .replaceAll("\\{\\{?.*?\\}?\\}", "")
          .replaceAll("\\[\\[[^\\]]+?(?::|\\|)(.+?)\\]\\]", "$1")
          .replaceAll("\\p{Punct}", ""),
        Normalizer.Form.NFD
      )
      .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
      .replaceAll("[^\\w\\s]", "")
      .toLowerCase
      .split(' ')
      .groupBy(x => x)
      .iterator
      .map(x => x._1 -> x._2.length)
      .filter(x => x._1.length > 0 && x._1.length <= WordsDbWriter.maxWordLength && x._2 > 1)
      .foreach { case (word, frequency) =>
        val key = wordToFileName(word)
        fileMap.synchronized {
          val writer = fileMap.getOrElseUpdate(key, new PrintWriter(new File(directory, key)))
          writer.println(word + "|" + frequency)
        }
      }
  }

  def close(): Unit = {
    fileMap.valuesIterator.foreach(_.close())
    fileMap.clear()
  }

}

object WordsDbWriter {

  val maxWordLength = 20

  def apply(directory: File): WordsDbWriter = {
    if (!directory.isDirectory) throw new IllegalArgumentException
    directory.listFiles().iterator.filter(_.isFile).foreach(_.delete())
    new WordsDbWriter(directory)
  }

}