package com.github.propi.wordsfetcher

import java.io.File

import scala.io.Source

/**
  * Created by propan on 21. 4. 2017.
  */
object ArticlesReader {

  case class Article(title: String, body: String)

  private def readNextArticle(it: Iterator[String], article: Article = Article("", "")): Option[Article] = if (it.hasNext) {
    val line = it.next().trim
    if (article.title.length > 0) {
      if (article.body.length > 0) {
        "^(.*?)(</text>)?$".r.findFirstMatchIn(line) match {
          case Some(rmatch) =>
            val newArticle = article.copy(body = article.body + " " + rmatch.group(1))
            if (rmatch.group(2) == null) {
              readNextArticle(it, newArticle)
            } else {
              Some(newArticle)
            }
          case None => readNextArticle(it, article)
        }
      } else {
        readNextArticle(it, article.copy(body = "^<text.*?>(.+)$".r.findFirstMatchIn(line).map(_.group(1)).getOrElse("")))
      }
    } else {
      readNextArticle(it, article.copy(title = "^<title>(.+)</title>$".r.findFirstMatchIn(line).map(_.group(1)).getOrElse("")))
    }
  } else {
    None
  }

  def reader(file: File)(f: Iterator[Article] => Unit): Unit = {
    val source = Source.fromFile(file)
    try {
      val it = source.getLines()
      val ait = new Iterator[Article] {
        var article = Option.empty[Article]

        def hasNext: Boolean = if (article.isEmpty) {
          article = readNextArticle(it)
          article.nonEmpty
        } else {
          true
        }

        def next(): Article = if (hasNext) {
          val x = article.get
          article = None
          x
        } else {
          Iterator.empty.next()
        }
      }
      f(ait)
    } finally {
      source.close()
    }
  }

}
