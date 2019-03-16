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
import akka.stream.scaladsl.Source

/**
  * Base interface for classes defining a collection of resources.
  */
sealed trait ResourceCollection {
  /**
    * Path to use for this collection. The path is treated as the id for referring to a
    * particular collection.
    */
  def path: String
}

/**
  * Collection that is generated from data in another source, e.g., AWS.
  */
trait SourceCollection extends ResourceCollection {
  /**
    * Source that generates a result set representing the resources available for the
    * collection.
    */
  def source: Source[ResultSet, NotUsed]
}

/**
  * Collection that is generated based on the results of one or more other collections. This
  * type is used for custom views of the data that are easier to work with than the raw data
  * from sources such as AWS.
  */
trait ViewCollection extends ResourceCollection {

  /**
    * Returns the paths for the other collections that are combined to generate this view.
    */
  def dependencies: Set[String]

  /**
    * Invoked by the manager to materialize the resources for this view. A view will not be
    * materialized until all dependency collections have been updated at least once. After that
    * it will be materialized after every update of a dependency collection.
    */
  def materialize(data: Map[String, ResourceMap]): ResourceMap
}
