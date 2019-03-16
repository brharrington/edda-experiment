/*
 * Copyright 2019 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.edda

import java.util.concurrent.ConcurrentHashMap

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.KillSwitch
import akka.stream.KillSwitches
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import com.netflix.iep.service.AbstractService
import com.netflix.spectator.api.Registry
import com.netflix.spectator.api.patterns.PolledMeter
import com.typesafe.scalalogging.StrictLogging

import scala.collection.SortedMap
import scala.collection.JavaConverters._

/**
  * Manages the state for a set of collections.
  */
class CollectionManager(registry: Registry, system: ActorSystem) extends AbstractService with StrictLogging {

  import java.util.{Set => JSet}

  private val collections = new ConcurrentHashMap[String, ResourceCollection]()

  private val dependents = new ConcurrentHashMap[String, JSet[String]]()

  private val data = new ConcurrentHashMap[String, ResultSet]()

  private var killSwitch: KillSwitch = _

  /**
    * Add a collection to the manager. Any dependencies for the collection must have already
    * been added.
    */
  def add(collection: ResourceCollection): CollectionManager = {
    collection match {
      case c: SourceCollection => addCollection(c)
      case c: ViewCollection   => addViewCollection(c)
    }
    this
  }

  private def checkIfPresent(path: String): Unit = {
    if (collections.containsKey(path)) {
      throw new IllegalStateException(s"collection already added for $path")
    }
  }

  private def addCollection(c: ResourceCollection): Unit = {
    checkIfPresent(c.path)
    collections.put(c.path, c)
    dependents.put(c.path, ConcurrentHashMap.newKeySet[String]())
    val initTime = registry.clock().wallTime()
    PolledMeter.using(registry)
      .withName("edda.collectionAge")
      .withTag("id", c.path)
      .monitorValue(c, (_: ResourceCollection) => {
        val updateTime = Option(data.get(c.path)).map(_.updateTime).getOrElse(initTime)
        (registry.clock().wallTime() - updateTime) / 1000.0
      })
    PolledMeter.using(registry)
      .withName("edda.collectionSize")
      .withTag("id", c.path)
      .monitorValue(c, (_: ResourceCollection) => {
        Option(data.get(c.path)).map(_.resources.size).getOrElse(0).toDouble
      })
  }

  private def addViewCollection(c: ViewCollection): Unit = {
    require(c.dependencies.nonEmpty, "view collections must have at least one dependency")
    val missingDeps = c.dependencies.filterNot(collections.containsKey)
    if (missingDeps.nonEmpty) {
      val missing = missingDeps.mkString(", ")
      throw new IllegalStateException(s"missing dependencies for ${c.path}: $missing")
    }
    addCollection(c)
    c.dependencies.foreach { d =>
      dependents.get(d).add(c.path)
    }
  }

  /** Return all of the paths for managed collections. */
  def paths: List[String] = {
    collections.keySet().asScala.toList.sorted
  }

  /** Get the current result set for a given path. */
  def get(path: String): Option[ResultSet] = {
    Option(data.get(path))
  }

  /** Get the results for all collections. */
  def getAll: SortedMap[String, ResultSet] = {
    SortedMap.empty[String, ResultSet] ++ data.asScala
  }

  private def allCollectionsInitialized: Boolean = {
    collections.keySet().stream().allMatch(k => data.containsKey(k))
  }

  private def allDependenciesInitialized(deps: Set[String]): Boolean = {
    collections.keySet().stream().filter(deps.contains).allMatch(k => data.containsKey(k))
  }

  /** Returns true if all connections have been updated at least once. */
  override def isHealthy: Boolean = {
    super.isHealthy && allCollectionsInitialized
  }

  private def updateCollections(result: ResultSet): Unit = {
    data.put(result.path, result)
    dependents.get(result.path).forEach { d =>
      val vc = collections.get(d).asInstanceOf[ViewCollection]
      if (allDependenciesInitialized(vc.dependencies)) {
        val dataMap = data.asScala
          .filterKeys(vc.dependencies.contains)
          .mapValues(_.resources)
          .toMap
        try {
          val rs = ResultSet(d, registry.clock().wallTime(), vc.materialize(dataMap))
          updateCollections(rs)
        } catch {
          case e: Exception => logger.error(s"failed to update view: $d", e)
        }
      }
    }
  }

  override def startImpl(): Unit = {
    implicit val materializer = ActorMaterializer()(system)
    val cs = collections.values().asScala.toList.collect {
      case c: SourceCollection => c
    }
    killSwitch = Source(cs)
      .flatMapMerge(cs.size, _.source)
      .viaMat(KillSwitches.single)(Keep.right)
      .toMat(Sink.foreach(updateCollections))(Keep.left)
      .run()
  }

  override def stopImpl(): Unit = {
    if (killSwitch != null) killSwitch.shutdown()
  }
}
