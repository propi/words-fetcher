package com.github.propi.wordsfetcher.util

import com.typesafe.config.ConfigFactory
import configs.syntax._
import configs.{Configs, Result}

/**
  * Created by Vaclav Zeman on 13. 8. 2017.
  */
object Conf {

  private val conf = ConfigFactory.load()

  def apply[A](path: String)(implicit a: Configs[A]): Result[A] = conf.get(path)

}