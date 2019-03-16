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
package com.netflix.edda.view

import com.fasterxml.jackson.databind.JsonNode
import com.netflix.edda.Resource
import com.netflix.edda.ResourceMap
import com.netflix.edda.ViewCollection

import scala.collection.JavaConverters._
import scala.collection.SortedMap

/**
  * The AWS DescribeInstances call returns a list of reservations that for the most part
  * are a useless grouping we do not care about. This view flattens the set of instances
  * and sets the id to the instance id to allow a particular instance to be looked up.
  */
class ViewInstancesCollection extends ViewCollection {

  override def path: String = "v2/view/instances"

  override def dependencies: Set[String] = Set("v2/aws/instances")

  private def toResource(instance: JsonNode): Resource = {
    Resource(instance.get("instanceId").textValue(), instance)
  }

  private def extractInstances(resource: Resource): List[Resource] = {
    val instances = resource.data.get("instances")
    instances.elements().asScala.map(toResource).toList
  }

  override def materialize(data: Map[String, ResourceMap]): ResourceMap = {
    SortedMap.empty[String, Resource] ++ data
      .values
      .flatMap(_.values)
      .flatMap(extractInstances)
      .map(r => r.id -> r)
  }
}
