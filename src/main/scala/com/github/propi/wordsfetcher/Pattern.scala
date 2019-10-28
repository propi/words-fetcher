package com.github.propi.wordsfetcher

import java.text.Normalizer

import com.github.propi.wordsfetcher.util.BasicExtractors.AnyToInt

import scala.collection.immutable.StringOps
import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 7. 1. 2018.
  */
sealed trait Pattern {
  val firstLetter: Option[Char]
  val length: Option[Int]
  val hasWildcard: Boolean
}

object Pattern {

  def unapply(arg: Pattern): Option[(Option[Char], Option[Int], Boolean)] = Some(arg.firstLetter, arg.length, arg.hasWildcard)

  /*trait HasFirstLetter {
    var firstLetter: Char
  }

  trait HasLength {
    var length: Int
  }

  trait HasWildcard*/

  /**
    * Found word must have same size as the pattern and must be fully matched
    *
    * @param letters pattern sequence
    */
  case class ExactPattern private(letters: IndexedSeq[PatternItem], firstLetter: Option[Char], length: Option[Int], hasWildcard: Boolean) extends Pattern {
    def norm(wordLength: Int): Iterator[ExactPattern] = {
      val ng = wordLength - letters.length
      if (ng > 0 && letters.contains(PatternItem.AnyWildcard)) {
        letters.iterator.zipWithIndex.collect {
          case (PatternItem.AnyWildcard, i) => Iterator.fill(ng)(i)
        }.flatten.toSeq.combinations(ng).map(_.groupBy(x => x).mapValues(_.size)).map { comb =>
          val normLetters = letters.iterator.zipWithIndex.flatMap {
            case (PatternItem.AnyWildcard, i) => Iterator.fill(1 + comb.getOrElse(i, 0))(PatternItem.AnyVariable)
            case (x, _) => Iterator(x)
          }.toVector
          ExactPattern.apply(normLetters)
        }
      } else {
        Iterator(this)
      }
    }
  }

  object ExactPattern {
    def apply(letters: IndexedSeq[PatternItem]): ExactPattern = {
      val firstChar = letters.headOption.collect {
        case PatternItem.Character(x) => x
      }
      val hasWildcard = letters.contains(PatternItem.AnyWildcard)
      new ExactPattern(letters, firstChar, Some(letters.length), hasWildcard)
    }
  }

  /**
    * Found word must contain all words from chars but with various ordering.
    * Optionally we can specify pattern for matching letters (from left to right).
    * The pattern need not be full.
    *
    * @param chars   chars for combinations
    * @param letters pattern
    */
  case class CombinationPattern(chars: IndexedSeq[Char], letters: IndexedSeq[PatternItem] = Vector()) extends Pattern {
    val firstLetter: Option[Char] = None
    val length: Option[Int] = None
    val hasWildcard: Boolean = false
  }

  /**
    * Found word must contain characters from chars (fully or partially - it depends on other properties)
    *
    * @param chars     chars for combinations
    * @param isSubWord if true then the found word must be subword of the chars, if false then the found word must be superword of the chars
    * @param set       if true then chars and word are converted into set (duplicit and ordering information are removed)
    * @param optLength length of the found word
    */
  case class VariousCombinationPattern(chars: IndexedSeq[Char], isSubWord: Boolean, set: Boolean, optLength: Option[Int] = None) extends Pattern {
    val firstLetter: Option[Char] = None
    val length: Option[Int] = None
    val hasWildcard: Boolean = false
  }

  sealed trait PatternItem

  object PatternItem {

    implicit class PimpedPatternItemSeq(x: IndexedSeq[PatternItem]) {
      def ~(patternItem: PatternItem): IndexedSeq[PatternItem] = x :+ patternItem

      def ~(patternItemSeq: IndexedSeq[PatternItem]): IndexedSeq[PatternItem] = x ++ patternItemSeq
    }

    implicit class PimpedPatternItem(x: PatternItem) extends PimpedPatternItemSeq(Vector(x))

    implicit class PimpedChar(x: Char) extends PimpedPatternItem(Character(x))

    implicit class PimpedInt(x: Int) extends PimpedPatternItem(Variable(x))

    case class Variable(index: Int)(val oneOf: Option[OneOf] = None, val noneOf: Option[NoneOf] = None) extends PatternItem {
      def ++(oneOf: OneOf): Variable = copy()(Some(oneOf), this.noneOf)

      def ++(noneOf: NoneOf): Variable = copy()(this.oneOf, Some(noneOf))

      def ++(noneOfPrev: NoneOf.Prev.type): Variable = copy()(this.oneOf, Some(NoneOf((1 until index).map(PatternItem.apply))))
    }

    object Variable {
      def apply(index: Int): Variable = new Variable(index)()
    }

    case object AnyVariable extends PatternItem

    case object AnyWildcard extends PatternItem

    case class Character(char: Char) extends PatternItem

    implicit def applySeq(string: String): IndexedSeq[Character] = {
      val ns: StringOps = Normalizer.normalize(
        string,
        Normalizer.Form.NFD
      ).replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
        .replaceAll("[^\\w?]", "")
        .toLowerCase
      ns.map(Character.apply)
    }

    implicit def apply(x: Int): Variable = Variable(x)

    implicit def apply(char: Char): Character = Character(char)

    def apply(pattern: String): IndexedSeq[PatternItem] = {
      @scala.annotation.tailrec
      def parseChar(chars: List[Char], result: IndexedSeq[PatternItem], inResult: IndexedSeq[PatternItem], isNeg: Boolean, isIn: Boolean): IndexedSeq[PatternItem] = chars match {
        case head :: tail => head match {
          case '[' =>
            val (num, rest) = tail.span(_ != ']')
            num.mkString match {
              case AnyToInt(x) => parseChar(rest.tail, result ~ x, inResult, isNeg, isIn)
              case _ => parseChar(rest.tail, result, inResult, isNeg, isIn)
            }
          case '(' => parseChar(tail, result, Vector.empty, isNeg, true)
          case '!' => parseChar(tail, result, inResult, true, isIn)
          case ')' => result.lastOption match {
            case Some(x: Variable) if inResult.nonEmpty =>
              val newVar = if (isNeg) x ++ NoneOf(inResult) else x ++ OneOf(inResult)
              parseChar(tail, result.init ~ newVar, Vector.empty, false, false)
            case Some(x: Variable) if isNeg => parseChar(tail, result.init ~ (x ++ NoneOf.Prev), Vector.empty, false, false)
            case _ => parseChar(tail, result, Vector.empty, false, false)
          }
          case AnyToInt(v) => if (isIn) {
            parseChar(tail, result, inResult ~ v, isNeg, isIn)
          } else {
            parseChar(tail, result ~ v, result, isNeg, isIn)
          }
          case '?' if !isIn => parseChar(tail, result ~ AnyVariable, inResult, isNeg, isIn)
          case '*' if !isIn => parseChar(tail, result ~ AnyWildcard, inResult, isNeg, isIn)
          case x => if (isIn) {
            parseChar(tail, result, inResult ~ x, isNeg, isIn)
          } else {
            parseChar(tail, result ~ x, inResult, isNeg, isIn)
          }
        }
        case Nil => result
      }

      parseChar(pattern.toCharArray.toList, Vector.empty, Vector.empty, false, false)
    }

    case class OneOf(items: Seq[PatternItem]) extends PatternItem

    object OneOf {
      def apply(item: PatternItem, items: PatternItem*): OneOf = new OneOf(item +: items)
    }

    case class NoneOf(items: Seq[PatternItem]) extends PatternItem

    object NoneOf {

      object Prev

      def apply(item: PatternItem, items: PatternItem*): NoneOf = new NoneOf(item +: items)
    }

  }

}
