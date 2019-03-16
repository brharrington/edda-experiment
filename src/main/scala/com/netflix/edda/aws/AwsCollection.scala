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
package com.netflix.edda.aws

import java.time.Duration
import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.stream.ThrottleMode
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Source
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.edda.Resource
import com.netflix.edda.ResultSet
import com.netflix.edda.SourceCollection
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.duration.FiniteDuration

/**
  * Base trait for collections that are based on AWS describe calls.
  */
trait AwsCollection extends SourceCollection with StrictLogging {

  private val mapper = new ObjectMapper()

  override def source: Source[ResultSet, NotUsed] = {
    val duration = FiniteDuration(delay.toMillis, TimeUnit.MILLISECONDS)
    Source.repeat(NotUsed)
      .throttle(1, duration, 1, ThrottleMode.Shaping)
      .via(Flow[NotUsed].flatMapConcat { _=>
        crawler
          .map { source =>
            source
              .fold(List.empty[Resource])((acc, r) => r :: acc)
              .recoverWithRetries(1, handleFailure)
          }
          .getOrElse(Source.empty)
      })
      .map{ resources =>
        ResultSet(path, resources)
      }
  }

  def toResource(id: String, obj: AnyRef): Resource = {
    Resource(id, mapper.valueToTree[JsonNode](obj))
  }

  def handleFailure: PartialFunction[Throwable, Source[List[Resource], NotUsed]] = {
    case e: Exception =>
      logger.error(s"failed to update aws collection: $path", e)
      Source.empty
  }

  /** The delay between attempts for running the crawler. */
  def delay: Duration

  /**
    * Single use source that will be used to get the set of current set of resources for
    * the AWS collection.
    */
  def crawler: Option[Source[Resource, NotUsed]]
}
