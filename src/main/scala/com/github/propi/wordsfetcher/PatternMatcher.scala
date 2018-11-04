package com.github.propi.wordsfetcher

import com.github.propi.wordsfetcher.WordsDb.Word
import PatternItemMatcher._

/**
  * Created by Vaclav Zeman on 7. 1. 2018.
  */
trait PatternMatcher[T <: Pattern] {
  val pattern: T

  def matchWord(word: Word): Boolean

  def filterFile(length: Int, firstChar: Char): Boolean
}

object PatternMatcher {

  type MMapper = collection.mutable.Map[Pattern.PatternItem, Pattern.PatternItem]
  type IMapper = collection.Map[Pattern.PatternItem, Pattern.PatternItem]

  trait PatternMatcherBuilder[T <: Pattern] {
    def build(pattern: T): PatternMatcher[T]
  }

  object PatternMatcherBuilder {
    implicit val exactPatternMatcherBuilder: PatternMatcherBuilder[Pattern.ExactPattern] = (pattern: Pattern.ExactPattern) => new ExactPatternMatcher(pattern)

    implicit val combinationPatternMatcherBuilder: PatternMatcherBuilder[Pattern.CombinationPattern] = (pattern: Pattern.CombinationPattern) => new CombinationPatternMatcher(pattern)

    implicit val variousCombinationPatternMatcherBuilder: PatternMatcherBuilder[Pattern.VariousCombinationPattern] = (pattern: Pattern.VariousCombinationPattern) => new VariousCombinationPatternMatcher(pattern)
  }

  trait VariableMapper[T <: Pattern] extends PatternMatcher[T] {
    def matchWord(word: Word): Boolean = matchWordWithMapper(word)(collection.mutable.HashMap.empty)

    def matchWordWithMapper(word: Word)(implicit mapper: MMapper): Boolean

    protected def compareCharWithPatternItem(implicit mapper: MMapper): (Char, Pattern.PatternItem) => Boolean = (char, patternItem) => {
      val result = patternItem.matchItem(char)
      if (result && patternItem.isInstanceOf[Pattern.PatternItem.Variable]) mapper += (patternItem -> Pattern.PatternItem.Character(char))
      result
    }
  }

  class ExactPatternMatcher(val pattern: Pattern.ExactPattern) extends VariableMapper[Pattern.ExactPattern] {
    def matchWordWithMapper(word: Word)(implicit mapper: MMapper): Boolean = word.word.length == pattern.letters.size &&
      word.word.toIterator.zip(pattern.letters.iterator).forall(compareCharWithPatternItem.tupled)

    def filterFile(length: Int, firstChar: Char): Boolean = {
      implicit val mapper: MMapper = collection.mutable.HashMap.empty
      pattern.letters.size == length && pattern.letters.headOption.exists(_.matchItem(firstChar))
    }
  }

  class CombinationPatternMatcher(val pattern: Pattern.CombinationPattern) extends VariableMapper[Pattern.CombinationPattern] {
    private val sortedChars = pattern.chars.sorted.mkString

    def matchWordWithMapper(word: Word)(implicit mapper: MMapper): Boolean = word.word.length == pattern.chars.size &&
      word.word.toIterator.take(pattern.letters.size).zip(pattern.letters.iterator).forall(compareCharWithPatternItem.tupled) &&
      word.word.sorted == sortedChars

    def filterFile(length: Int, firstChar: Char): Boolean = {
      implicit val mapper: MMapper = collection.mutable.HashMap.empty
      pattern.chars.size == length && pattern.letters.headOption.forall(_.matchItem(firstChar))
    }
  }

  class VariousCombinationPatternMatcher(val pattern: Pattern.VariousCombinationPattern) extends PatternMatcher[Pattern.VariousCombinationPattern] {
    private lazy val setChars = pattern.chars.toSet

    def matchWord(word: Word): Boolean = {
      pattern.optLength.forall(_ == word.word.length) && (if (pattern.set) {
        val charSet = word.word.toSet
        if (pattern.isSubWord) {
          charSet.subsetOf(setChars)
        } else {
          setChars.subsetOf(charSet)
        }
      } else {
        if (pattern.isSubWord) {
          word.word.diff(pattern.chars).isEmpty
        } else {
          pattern.chars.diff(word.word).isEmpty
        }
      })
    }

    def filterFile(length: Int, firstChar: Char): Boolean = pattern.optLength.forall(_ == length) && (
      (pattern.isSubWord && !pattern.set && length <= pattern.chars.size) ||
        (pattern.isSubWord && pattern.set && length <= setChars.size) ||
        (!pattern.isSubWord && !pattern.set && length >= pattern.chars.size) ||
        (!pattern.isSubWord && pattern.set && length >= setChars.size)
      ) && (
      (pattern.isSubWord && pattern.chars.contains(firstChar)) ||
        !pattern.isSubWord
      )
  }

}