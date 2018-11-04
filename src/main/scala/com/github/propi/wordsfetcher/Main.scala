package com.github.propi.wordsfetcher

import com.github.propi.wordsfetcher.Pattern.ExactPattern
import com.github.propi.wordsfetcher.Pattern.PatternItem.NoneOf

import com.github.propi.wordsfetcher.Pattern.ExactPattern
import com.github.propi.wordsfetcher.Pattern.PatternItem.{NoneOf, _}

/**
  * Created by propan on 21. 4. 2017.
  */
object Main {

  def main(args: Array[String]): Unit = {
    //word searching

    /*val x = "xyz"
    for {
      c1 <- x
      c2 <- x
      c3 <- x
    } {
      val map = List(c1, c2, c3).groupBy(x => x).mapValues(_.size)
      if (map.get('y').forall(_ == 1) && map.get('z').forall(_ == 1)) {
        println("" + c1 + c2 + c3)
      }
    }*/


    //val pattern = ExactPattern(1 ~ (2 ++ NoneOf(1)) ~ (3 ++ NoneOf(1 ~ 2)) ~ 'o' ~ (4 ++ NoneOf(1 ~ 2 ~ 3)) ~ 2 ~ (6 ++ NoneOf(1 ~ 2 ~ 3 ~ 4 ~ 5)) ~ (7 ++ NoneOf(1 ~ 2 ~ 3 ~ 5 ~ 6)) ~ (8 ++ NoneOf(1 ~ 2 ~ 3 ~ 5 ~ 6 ~ 7)))
    //val pattern = ExactPattern(1 ~ 2 ~ 3 ~ 'd' ~ 'a' ~ 's' ~ 'a')
    /*WordsDb.DbMode(WordsDb.DbMode.DiscAll, pattern) { implicit dbmode =>
      WordsDb.searchWord(pattern).foreach(println)
    }
    val pattern1 = ExactPattern(1 ~ 2 ~ 3 ~ 1 ~ 4 ~ 5 ~ 2 ~ 6 ~ 7)*/
    /*val pattern2 = ExactPattern(8 ~ 9 ~ 10 ~ 7 ~ 11 ~ 8 ~ 1)
    val pattern3 = ExactPattern(6 ~ 9 ~ 12 ~ 4 ~ 7 ~ 11 ~ 7)
    WordsDb.DbMode(WordsDb.DbMode.DiscAll, pattern2) { implicit dbmode =>
      for {
        //(word1, mapper) <- WordsDb.searchWordWithMapper(pattern1).par
        (word2, mapper2) <- WordsDb.searchWordWithMapper(pattern2/*, mapper*/).par if word2.frequency > 100
        (word3, _) <- WordsDb.searchWordWithMapper(pattern3, mapper2) if word3.frequency > 100
      } {
        println(/*word1 + " - " + */word2 + " - " + word3)
      }
    }*/

    //sentence searching
    /*val pattern1 = ExactPattern((1 ++ NoneOf("0123456789")) ~ (2 ++ NoneOf(1 ~ "0123456789")) ~ 2 ~ 1)
    val pattern2 = ExactPattern(2 ~ AnyVariable ~ AnyVariable ~ 2)
    WordsDb.DbMode(WordsDb.DbMode.DiscAll, pattern1) { implicit dbmode =>
      for {
        (word1, mapper) <- WordsDb.searchWordWithMapper(pattern1).par
        (word2, _) <- WordsDb.searchWordWithMapper(pattern2, mapper)
      } {
        println(word1 + " - " + word2)
      }
    }*/

    //sorted sentence searching
    /*WordsDb.DbMode(WordsDb.DbMode.DiscAll, pattern1) { implicit dbmode =>
      val a = for {
        (word1, mapper) <- WordsDb.searchWordWithMapper(pattern1).sortBy(_._1.frequency)(implicitly[Ordering[Int]].reverse).take(5).par
        (word2, _) <- WordsDb.searchWordWithMapper(pattern2, mapper).sortBy(_._1.frequency)(implicitly[Ordering[Int]].reverse).take(5)
      } yield {
        (word1, word2)
      }
      a.seq.sortBy(x => (x._1.frequency, x._2.frequency)).foreach(println)
    }*/
  }

  /*ArticlesReader.reader(new File("cswiki-20170320-pages-articles.xml")) { it =>
    WordsDb.save { wdw =>
      var i = 0
      it.map(article => article.title + " " + article.body).grouped(1000).foreach { buffer =>
        buffer.grouped(100).toList.par.map(_.reduce(_ + " " + _)).foreach { text =>
          wdw.save(text)
        }
        i += 1000
        println("processed articles: " + i)
      }
    }
  }*/

  //WordsDb.searchByPattern("v?1s?2k").foreach(println)


  //val patternItems = PatternItem.Character("vr") & PatternItem.Variable(1) & PatternItem.AnyVariable & PatternItem.Variable(1)
  /*val pattern: ExactPattern = new ExactPattern(patternItems) with Pattern.HasFirstLetter with Pattern.HasLength {
    var firstLetter: Char = 'v'
    var length: Int = this.letters.size
  }*/

  //val pattern: VariousCombinationPattern = VariousCombinationPattern("litizubri", false, true, None)

  /*def a(x: String) = PatternItem.OneOf(x.map(PatternItem.Character.apply))

  val p1 = a("et")
  val p2 = a("aimn")
  val p3 = a("dgkorsuw")
  val p4 = a("bcfhjlpqvxyz")

  val pattern: ExactPattern = ExactPattern(p3 & p3 & p3 & p4 & p1 & p3 & p3)
  val pattern2: ExactPattern = ExactPattern(p4 & p4 & p4 & p3 & p3 & p2 & p1)
  val pattern3: ExactPattern = ExactPattern(p3 & p4 & p2 & p4 & p1)
  val pattern4: ExactPattern = ExactPattern(p2 & p3 & p3 & p3 & p1 & p4 & p3)

  val a = Vector(
    PatternItem.Character('a'),
    PatternItem.Character('d'),
    PatternItem.AnyVariable,
  PatternItem.Character('b'),
  PatternItem.AnyVariable,
  PatternItem.Character('y'),
  PatternItem.AnyVariable,
  PatternItem.AnyVariable,
  PatternItem.Character('z'))

  val pattern5: ExactPattern = ExactPattern(a)*/

  //val pattern: ExactPattern = ExactPattern(PatternItem.OneOf(Seq(PatternItem.)) & PatternItem.AnyVariable & PatternItem.Character('j') & PatternItem.Character('e') & PatternItem.Character('k') & PatternItem.AnyVariable)

  /*val list = WordsDb.searchByPattern("p?2p?3?4?5?6?2?7")

  list.filter(x => Set(x.word(3), x.word(4), x.word(5), x.word(6), x.word(8)).size == 5)
    .map(x => x.word -> x.frequency).filter(_._1.toSet.size == 7).sortBy(_._2)(Ordering.Int.reverse).foreach(println)*/

  /*val list = WordsDb.searchByPattern("?1a?2t?3?4?5")

  list.filter(x => Set(x.word(0), x.word(2), x.word(4), x.word(5), x.word(6)).size == 5)
    .map(x => x.word -> x.frequency)
    .filter(_._1.toSet.size == 7)
    .sortBy(_._2)(Ordering.Int.reverse).foreach(println)*/


  //list.foreach(println)

}
