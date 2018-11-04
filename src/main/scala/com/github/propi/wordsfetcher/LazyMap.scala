package com.github.propi.wordsfetcher

/**
  * Created by Vaclav Zeman on 13. 4. 2018.
  */
class LazyMap[K, V] private(private var map: collection.Map[K, V]) extends collection.mutable.Map[K, V] {
  private val mutableMap = collection.mutable.HashMap.empty[K, V]

  def +=(kv: (K, V)): LazyMap.this.type = {
    mutableMap += kv
    this
  }

  def -=(key: K): LazyMap.this.type = {
    mutableMap -= key
    map = map - key
    this
  }

  def get(key: K): Option[V] = mutableMap.get(key).orElse(map.get(key))

  def iterator: Iterator[(K, V)] = mutableMap.iterator ++ map.iterator
}

object LazyMap {

  implicit class PimpedMap[K, V](map: collection.Map[K, V]) {
    def toMutableMap: collection.mutable.Map[K, V] = new LazyMap(map)
  }

}
