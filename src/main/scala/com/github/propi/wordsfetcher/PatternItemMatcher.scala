package com.github.propi.wordsfetcher

import com.github.propi.wordsfetcher.Pattern.PatternItem
import com.github.propi.wordsfetcher.PatternMatcher.IMapper

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 12. 1. 2018.
  */
trait PatternItemMatcher[T <: Pattern.PatternItem] {
  val pattern: T

  def matchItem(char: Char): Boolean
}

object PatternItemMatcher {

  implicit def apply(patternItem: PatternItem)(implicit mapper: IMapper): PatternItemMatcher[_] = mapper.getOrElse(patternItem, patternItem) match {
    case PatternItem.AnyVariable | PatternItem.AnyWildcard => new AlwaysTruePatternItemMatcher(patternItem)
    case x: PatternItem.Variable =>
      val oneOf = x.oneOf.map(new OneOfPatternItemMatcher(_)).getOrElse(new AlwaysTruePatternItemMatcher(patternItem))
      val noneOf = x.noneOf.map(new NoneOfPatternItemMatcher(_)).getOrElse(new AlwaysTruePatternItemMatcher(patternItem))
      new CompoundPatternItemMatcher(x, oneOf, noneOf)
    case x: PatternItem.Character => new CharacterPatternItemMatcher(x)
    case x: PatternItem.OneOf => new OneOfPatternItemMatcher(x)
    case x: PatternItem.NoneOf => new NoneOfPatternItemMatcher(x)
  }

  trait PatternItemMatcherBuilder[T <: Pattern.PatternItem] {
    def build(pattern: T): PatternItemMatcher[T]
  }

  class CompoundPatternItemMatcher(val pattern: PatternItem, patterMatcher1: PatternItemMatcher[_], patterMatcher2: PatternItemMatcher[_]) extends PatternItemMatcher[PatternItem] {
    def matchItem(char: Char): Boolean = patterMatcher1.matchItem(char) && patterMatcher2.matchItem(char)
  }

  class AlwaysTruePatternItemMatcher(val pattern: PatternItem) extends PatternItemMatcher[PatternItem] {
    def matchItem(char: Char): Boolean = true
  }

  class CharacterPatternItemMatcher(val pattern: PatternItem.Character) extends PatternItemMatcher[PatternItem.Character] {
    def matchItem(char: Char): Boolean = char == pattern.char
  }

  class OneOfPatternItemMatcher(val pattern: PatternItem.OneOf)(implicit patternToPatternMatcher: PatternItem => PatternItemMatcher[_]) extends PatternItemMatcher[PatternItem.OneOf] {
    def matchItem(char: Char): Boolean = pattern.items.exists(patternToPatternMatcher(_).matchItem(char))
  }

  class NoneOfPatternItemMatcher(val pattern: PatternItem.NoneOf)(implicit patternToPatternMatcher: PatternItem => PatternItemMatcher[_]) extends PatternItemMatcher[PatternItem.NoneOf] {
    def matchItem(char: Char): Boolean = !pattern.items.exists(patternToPatternMatcher(_).matchItem(char))
  }

}