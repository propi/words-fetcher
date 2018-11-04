package com.github.propi.wordsfetcher

import spray.json._

import spray.json.RootJsonFormat

/**
  * Created by Vaclav Zeman on 4. 11. 2018.
  */
object JsonFormats extends DefaultJsonProtocol {

  implicit val wordJsonFormat: RootJsonFormat[WordsDb.Word] = jsonFormat2(WordsDb.Word)

}