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

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.netflix.edda.Resource
import software.amazon.awssdk.services.ec2.Ec2AsyncClient
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest

import scala.collection.JavaConverters._

/**
  * Collection based on DescribeInstances. For more convenient usage without needing to
  * worry about reservations, see ViewInstancesCollection.
  */
class InstancesCollection(client: Ec2AsyncClient) extends AwsCollection {

  private val request = DescribeInstancesRequest.builder()
    .maxResults(1000)
    .build()

  override def path: String = "v2/aws/instances"

  override def delay: Duration = Duration.ofSeconds(30)

  override def crawler: Option[Source[Resource, NotUsed]] = {
    val src = Source.fromPublisher(client.describeInstancesPaginator(request))
      .flatMapConcat { response =>
        Source(response.reservations().asScala.toList)
      }
      .map { reservation =>
        toResource(reservation.reservationId(), reservation.toBuilder)
      }
    Some(src)
  }
}
