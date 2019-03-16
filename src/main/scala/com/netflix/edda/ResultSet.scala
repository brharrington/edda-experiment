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

import scala.collection.SortedMap

/**
  * Cached results for a given collection.
  *
  * @param path
  *     Path relative to `api` for where to host the results.
  * @param updateTime
  *     Timestamp for when this result set was last updated. Used to track the staleness
  *     of the collection.
  * @param resources
  *     Set of resources that were available as of the last update.
  */
case class ResultSet(path: String, updateTime: Long, resources: ResourceMap)

object ResultSet {
  def apply(path: String, resource: List[Resource]): ResultSet = {
    val map = resource.groupBy(_.id).map(t => t._1 -> t._2.head)
    ResultSet(path, System.currentTimeMillis(), SortedMap.empty[String, Resource] ++ map)
  }
}
