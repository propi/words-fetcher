package com.github.propi.wordsfetcher

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.{ActorMaterializer, Materializer}
import com.github.propi.wordsfetcher.JsonFormats._
import com.github.propi.wordsfetcher.PatternMatcher.IMapper
import com.github.propi.wordsfetcher.PatternMatcher.PatternMatcherBuilder._
import com.github.propi.wordsfetcher.util.{DefaultServer, DefaultServerConf}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn
import scala.language.postfixOps

/**
  * Created by Vaclav Zeman on 4. 11. 2018.
  */
object MainWeb extends DefaultServer with DefaultServerConf {

  val confPrefix = "wf"
  val configServerPrefix: String = s"$confPrefix.server"

  implicit val actorSystem: ActorSystem = ActorSystem("wf-http")
  implicit val materializer: Materializer = ActorMaterializer()

  implicit lazy val dbMode: WordsDb.DbMode = WordsDb.DbMode.all

  private var stopCompute = false

  private def usePatterns[A <: Pattern](minFreq: Option[Int], topK: Option[Int], intopK: Option[Int])(patterns: A*)(implicit pb: PatternMatcher.PatternMatcherBuilder[A]) = {
    val search = Function.chain[IndexedSeq[(WordsDb.Word, IMapper)]](List(
      x => minFreq.map(mf => x.filter(_._1.frequency >= mf)).getOrElse(x),
      //x => x.sortBy(_._1.frequency)(implicitly[Ordering[Int]].reverse),
      x => intopK.map(tk => x.take(tk)).getOrElse(x)
    ))
    def usePattern(patterns: List[A], result: IndexedSeq[WordsDb.Word] = Vector.empty, map: PatternMatcher.IMapper = Map.empty): Seq[IndexedSeq[WordsDb.Word]] = patterns match {
      case head :: tail =>
        val words = search(WordsDb.searchWordWithMapper(head, map))
        (if (map.isEmpty) words.par else words).flatMap { case (word, mapper) =>
          if (stopCompute) List(result :+ word) else usePattern(tail, result :+ word, mapper)
        }.seq
      case Nil => List(result)
    }

    stopCompute = false
    val result = usePattern(patterns.toList)
    stopCompute = false
    topK.map(tk => result.take(tk)).getOrElse(result).take(2000)
  }

  val route: Route = parameters('patterns, 'minf.as[Int].?, 'topk.as[Int].?, 'intopk.as[Int].?) { (patterns, minf, topk, intopk) =>
    path("exact") {
      val words = usePatterns(minf, topk, intopk)(patterns.split(' ').map(_.trim).map(Pattern.PatternItem.apply).map(x => Pattern.ExactPattern(x)).toSeq: _*)
      complete(words)
    } ~ path("comb") {
      parameter('chars) { chars =>
        val words = if (patterns.isEmpty) {
          usePatterns(minf, topk, intopk)(Pattern.CombinationPattern(chars))
        } else {
          usePatterns(minf, topk, intopk)(patterns.split(' ').map(_.trim).map(Pattern.PatternItem.apply).map(x => Pattern.CombinationPattern(chars, x)).toSeq: _*)
        }
        complete(words)
      }
    } ~ path("varcomb") {
      parameters('issub.as[Boolean], 'isset.as[Boolean], 'len.as[Int].?) { (issub, isset, len) =>
        val words = usePatterns(minf, topk, intopk)(patterns.split(' ').map(_.trim).map(x => Pattern.VariousCombinationPattern(x, issub, isset, len)).toSeq: _*)
        complete(words)
      }
    }
  } ~ path("sc") {
    stopCompute = true
    complete("")
  }

  def main(args: Array[String]): Unit = {
    Await.result(bind(), 30 seconds)
    if (stoppingToken.trim.isEmpty) {
      println("Press enter to exit: ")
      StdIn.readLine()
      Await.result(stop(), 30 seconds)
    }
  }

}
