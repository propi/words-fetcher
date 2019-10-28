package com.github.propi.wordsfetcher

import java.io.{File, PrintWriter}

import com.github.propi.wordsfetcher.LazyMap._
import com.github.propi.wordsfetcher.PatternMatcher.PatternMatcherBuilder._
import com.github.propi.wordsfetcher.PatternMatcher.{IMapper, MMapper, PatternMatcherBuilder}

import scala.io.Source
import scala.util.Try

/**
  * Created by propan on 21. 4. 2017.
  */
object WordsDb {

  private val directory = new File("data")

  case class Word(word: String, /*variables: Map[Int, Char],*/ frequency: Int) {
    override def toString: String = s"$word ($frequency)"
  }

  def save(f: WordsDbWriter => Unit): Unit = {
    val wordsDbWriter = WordsDbWriter(directory)
    try {
      f(wordsDbWriter)
    } finally {
      wordsDbWriter.close()
    }
  }

  def groupAndSort(): Unit = {
    var i = 0
    directory.listFiles().iterator.filter(_.isFile).foreach { file =>
      val source = Source.fromFile(file)
      val words = try {
        source.getLines()
          .map(_.split('|'))
          .filter(_.length == 2)
          .map(x => x(0) -> x(1).toInt)
          .toList
          .groupBy(_._1)
          .map(x => x._1 -> x._2.iterator.map(_._2).sum)
          .toList
          .sortBy(_._2)(Ordering.Int.reverse)
      } finally {
        source.close()
        file.delete()
      }
      val pw = new PrintWriter(file)
      try {
        words.iterator.map(x => x._1 + "|" + x._2).foreach(pw.println)
      } finally {
        pw.close()
      }
      i += 1
      println("processed files: " + i)
    }
  }

  sealed trait DbMode {
    def wordsDb(patternMatcher: PatternMatcher[_]): TraversableOnce[Word]
  }

  object DbMode {

    object Disc extends DbMode {
      def wordsDb(patternMatcher: PatternMatcher[_]): TraversableOnce[Word] = directory.listFiles().view.filter { file =>
        val x = file.getName.split('-')
        if (x.length == 2) {
          val length = Try(x(0).toInt).toOption
          val first = x(1).headOption
          length.zip(first).headOption.exists((patternMatcher.filterFile _).tupled)
        } else {
          false
        }
      }.flatMap { file =>
        new Traversable[Word] {
          def foreach[U](f: Word => U): Unit = {
            val source = Source.fromFile(file)
            try {
              source.getLines()
                .map(_.split('|'))
                .filter(_.length == 2)
                .map(x => Word(x(0), x(1).toInt))
                .foreach(f)
            } finally {
              source.close()
            }
          }
        }
      }
    }

    object DiscAll extends DbMode {
      def wordsDb(patternMatcher: PatternMatcher[_]): TraversableOnce[Word] = directory.listFiles().view.flatMap { file =>
        new Traversable[Word] {
          def foreach[U](f: Word => U): Unit = {
            val source = Source.fromFile(file)
            try {
              source.getLines()
                .map(_.split('|'))
                .filter(_.length == 2)
                .map(x => Word(x(0), x(1).toInt))
                .foreach(f)
            } finally {
              source.close()
            }
          }
        }
      }
    }

    class InMemory(cache: collection.Map[Char, collection.Map[Int, IndexedSeq[Word]]]) extends DbMode {
      def wordsDb(patternMatcher: PatternMatcher[_]): TraversableOnce[Word] = patternMatcher.pattern match {
        case Pattern(Some(firstLetter), Some(length), false) => cache.get(firstLetter).flatMap(_.get(length)).getOrElse(Iterator.empty)
        case Pattern(Some(firstLetter), Some(length), true) => cache.get(firstLetter).iterator.flatMap(x => x.keysIterator.filter(_ >= length).flatMap(x.apply))
        case Pattern(Some(firstLetter), _, _) => cache.get(firstLetter).map(_.valuesIterator.flatten).getOrElse(Iterator.empty)
        case Pattern(None, Some(length), true) => cache.valuesIterator.flatMap(x => x.keysIterator.filter(_ >= length).flatMap(x.apply))
        case Pattern(None, Some(length), false) => cache.valuesIterator.flatMap(_.get(length).map(_.iterator).getOrElse(Iterator.empty))
        case _ => cache.valuesIterator.flatMap(_.valuesIterator.flatten)
      }
    }

    def apply[T, P <: Pattern](dbMode: DbMode, pattern: P)(f: DbMode => T)(implicit patternMatcherBuilder: PatternMatcherBuilder[P]): T = {
      val map = collection.mutable.HashMap.empty[Char, collection.mutable.HashMap[Int, collection.mutable.ArrayBuffer[Word]]]
      dbMode.wordsDb(patternMatcherBuilder.build(pattern)).toIterator
        .map(x => (x.word.head, x.word.length, x))
        .foreach { case (f, l, w) =>
          map.getOrElseUpdate(f, collection.mutable.HashMap.empty).getOrElseUpdate(l, collection.mutable.ArrayBuffer.empty) += w
        }
      f(new InMemory(map))
    }

    def all: DbMode = {
      val map = collection.mutable.HashMap.empty[Char, collection.mutable.HashMap[Int, collection.mutable.ArrayBuffer[Word]]]
      DiscAll.wordsDb(exactPatternMatcherBuilder.build(Pattern.ExactPattern(IndexedSeq.empty))).toIterator
        .map(x => (x.word.head, x.word.length, x))
        .foreach { case (f, l, w) =>
          map.getOrElseUpdate(f, collection.mutable.HashMap.empty).getOrElseUpdate(l, collection.mutable.ArrayBuffer.empty) += w
        }
      new InMemory(map)
    }

  }

  def searchWord[T <: Pattern](pattern: T)(implicit patternMatcherBuilder: PatternMatcherBuilder[T], dbMode: DbMode = DbMode.Disc): IndexedSeq[Word] = {
    val patternMatcher = patternMatcherBuilder.build(pattern)
    dbMode.wordsDb(patternMatcher).filter(patternMatcher.matchWord).toVector.sortBy(_.frequency)(implicitly[Ordering[Int]].reverse)
  }

  def searchWordWithMapper[T <: Pattern](pattern: T, mapper: IMapper = Map.empty)(implicit patternMatcherBuilder: PatternMatcherBuilder[T], dbMode: DbMode = DbMode.Disc): IndexedSeq[(Word, IMapper)] = {
    val patternMatcher = patternMatcherBuilder.build(pattern)
    val wordMatcher: Word => Option[(Word, IMapper)] = patternMatcher match {
      case x: PatternMatcher.VariableMapper[_] => word => {
        implicit val newMapper: MMapper = mapper.toMutableMap
        if (x.matchWordWithMapper(word)) Some(word -> newMapper) else None
      }
      case x => word => if (x.matchWord(word)) Some(word -> mapper) else None
    }
    dbMode.wordsDb(patternMatcher).flatMap(wordMatcher(_)).toVector.sortBy(_._1.frequency)(implicitly[Ordering[Int]].reverse)
  }

}

/*private def stringMatch(pattern: List[Any], word: Word) = {
  pattern.length == word.word.length && pattern.zip(word.word).forall {
    case (x: Int, y) => word.variables.get(x).forall(_ == y)
    case (x, y) => x == y
  }
}*/

//val queue = collection.mutable.HashMap.empty[Int, collection.mutable.HashMap[Char, List[Word]]]

/*def searchByPattern(pattern: String): List[Word] = {
  val npattern = {
    val npattern = Normalizer.normalize(
      pattern,
      Normalizer.Form.NFD
    ).replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
      .replaceAll("[^\\w?]", "")
      .toLowerCase
    val (x, y) = npattern.foldLeft(Option.empty[String], List.empty[Any]) {
      case ((None, result), '?') => (Some(""), result)
      case ((Some(n), result), '?') => (Some(""), n.toInt :: result)
      case ((Some(n), result), x) => if (x.isDigit) (Some(n + x), result) else (None, x :: n.toInt :: result)
      case ((_, result), x) => (None, x :: result)
    }
    x.map(_.toInt :: y).getOrElse(y).reverse
  }
  if (npattern.nonEmpty) {
    if (queue.get(npattern.length).isEmpty) {
      val files = directory.listFiles().iterator
      val searchFiles = files.filter(_.getName.startsWith(npattern.length + "-"))
      searchFiles.foreach { file =>
        val source = Source.fromFile(file)
        try {
          val data = source.getLines()
            .map(_.split('|'))
            .filter(_.length == 2)
            .map(x => Word(x(0), Map.empty, x(1).toInt))
            .toList
            .groupBy(_.word.head)
          npattern.length -> (queue.getOrElseUpdate(npattern.length, collection.mutable.HashMap(data.toList: _*)) ++= data)
        } finally {
          source.close()
        }
      }
    }
    queue.get(npattern.length).map { map =>
      val words = npattern.head match {
        case x: Int => map.valuesIterator.flatten.toList
        case x: Char => map.getOrElse(x, Nil)
        case _ => Nil
      }
      words.iterator.map(x => x.copy(variables = npattern.zip(x.word).iterator.collect({ case (x: Int, y) => x -> y }).toMap)).filter(x => stringMatch(npattern, x)).toList
    }.getOrElse(Nil)
  } else {
    Nil
  }
}*/