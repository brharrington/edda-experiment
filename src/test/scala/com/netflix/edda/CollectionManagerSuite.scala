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

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.netflix.edda.CollectionManagerSuite._
import com.netflix.spectator.api.DefaultRegistry
import org.scalatest.FunSuite

class CollectionManagerSuite extends FunSuite {

  private val registry = new DefaultRegistry()
  private val system = ActorSystem(getClass.getSimpleName)

  private val mapper = new ObjectMapper()

  private def createData(k: String, n: Int, annotations: List[String] = Nil): ResultSet = {
    val resources = (0 until n).toList.map { i =>
      val data = mapper.createObjectNode()
      data.put(k, i)
      annotations.foreach { annotation =>
        data.put(annotation, true)
      }
      Resource(i.toString, data)
    }
    ResultSet(k, resources).copy(updateTime = 0)
  }

  test("single source collection") {
    val mgr = new CollectionManager(registry, system)
    mgr.add(OkSourceCollection("test", List(createData("test", 1))))
    assert(!mgr.isHealthy)
    mgr.start()
    while (!mgr.isHealthy) {
      Thread.sleep(100)
    }
    assert(mgr.get("test") === Some(createData("test", 1)))
    mgr.stop()
  }

  test("view collection missing dependency") {
    val mgr = new CollectionManager(registry, system)
    intercept[IllegalStateException] {
      mgr.add(OkViewCollection("test", Set("a")))
    }
  }

  test("view collection already added") {
    val manager = new CollectionManager(registry, system)
    intercept[IllegalStateException] {
      manager
        .add(OkSourceCollection("test", List(createData("test", 1))))
        .add(OkViewCollection("view", Set("test")))
        .add(OkViewCollection("view", Set("test")))
    }
  }

  test("view collection cycle") {
    val manager = new CollectionManager(registry, system)
    intercept[IllegalStateException] {
      manager
        .add(OkSourceCollection("test", List(createData("test", 1))))
        .add(OkViewCollection("view", Set("test", "view")))
    }
  }

  test("view collection ok") {
    val manager = new CollectionManager(registry, system)
    manager
      .add(OkSourceCollection("test", List(createData("test", 1))))
      .add(OkViewCollection("view", Set("test")))
    manager.start()
    while (!manager.isHealthy) {
      Thread.sleep(100)
    }
    assert(manager.get("test") === Some(createData("test", 1, List("view"))))
    manager.stop()
  }

  test("view collection transitive") {
    val manager = new CollectionManager(registry, system)
    manager
      .add(OkSourceCollection("test", List(createData("test", 1))))
      .add(OkViewCollection("a", Set("test")))
      .add(OkViewCollection("b", Set("a")))
    assert(!manager.isHealthy)
    manager.start()
    while (!manager.isHealthy) {
      Thread.sleep(100)
    }
    assert(manager.get("test") === Some(createData("test", 1, List("a", "b"))))
    manager.stop()
  }

  test("view collection temporary failure") {
    val manager = new CollectionManager(registry, system)
    val rs = createData("test", 1)
    manager
      .add(OkSourceCollection("test", List(rs, rs)))
      .add(FailureViewCollection(OkViewCollection("view", Set("test"))))
    manager.start()
    while (!manager.isHealthy) {
      Thread.sleep(100)
    }
    assert(manager.get("test") === Some(createData("test", 1, List("view"))))
    manager.stop()
  }
}

object CollectionManagerSuite {

  case class OkSourceCollection(path: String, data: List[ResultSet]) extends SourceCollection {
    override def source: Source[ResultSet, NotUsed] = {
      Source(data)
    }
  }

  case class OkViewCollection(path: String, dependencies: Set[String]) extends ViewCollection {

    private def annotate(resource: Resource): Resource = {
      val data = resource.data.asInstanceOf[ObjectNode]
      data.put(path, true)
      resource.copy(data = data)
    }

    override def materialize(data: Map[String, ResourceMap]): ResourceMap = {
      val combined = data.values.reduce((a, b) => a ++ b)
      combined.map {
        case (id, r) => id -> annotate(r)
      }
    }
  }

  case class FailureViewCollection(c: ViewCollection) extends ViewCollection {
    @volatile private var failedOnce: Boolean = false
    override def path: String = c.path
    override def dependencies: Set[String] = c.dependencies
    override def materialize(data: Map[String, ResourceMap]): ResourceMap = {
      if (!failedOnce) {
        failedOnce = true
        throw new RuntimeException(s"failed to materialize $path")
      }
      c.materialize(data)
    }
  }


}
