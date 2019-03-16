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
import java.util.concurrent.ConcurrentHashMap

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.netflix.edda.CollectionManager
import com.netflix.edda.Resource
import com.netflix.edda.ResultSet
import software.amazon.awssdk.services.elasticloadbalancing.ElasticLoadBalancingAsyncClient
import software.amazon.awssdk.services.elasticloadbalancing.model.DescribeLoadBalancerAttributesRequest

import scala.compat.java8.FutureConverters._

/**
  * Collection based on the v1 DescribeLoadBalancerAttributes. AWS requires one call per load
  * balancer. This collection will include the results for all load balancers and wraps the
  * LoadBalancerAttribute object with the name. This allows the attributes to be correlated with
  * the respective load balancer when the full listing is retrieved.
  */
class LoadBalancerAttributesCollection(
  mgr: CollectionManager,
  client: ElasticLoadBalancingAsyncClient
) extends AwsCollection {

  import LoadBalancerAttributesCollection._

  private val cache = new ConcurrentHashMap[String, Attributes]()

  override def path: String = "v2/aws/loadBalancerAttributes"

  override def delay: Duration = Duration.ofSeconds(10)

  private def loadBalancerNames(rs: ResultSet): List[String] = {
    rs.resources
      .values
      .map(_.data.get("loadBalancerName").textValue())
      .toList
  }

  override def crawler: Option[Source[Resource, NotUsed]] = {
    mgr.get("v2/aws/loadBalancers")
      .map { rs =>
        Source(loadBalancerNames(rs))
          .flatMapConcat { lb =>
            // Cache the set of load balancers to reduce the number of calls to AWS. This is
            // done here rather than via setting a larger delay so that the attributes for new
            // load balancers will be picked up quickly.
            val attrs = cache.get(lb)
            if (attrs != null && attrs.isRecent) {
              attrs.source
            } else {
              val request = DescribeLoadBalancerAttributesRequest.builder()
                .loadBalancerName(lb)
                .build()
              val future = toScala(client.describeLoadBalancerAttributes(request))
              Source.fromFuture(future)
                .map { response =>
                  val data = new java.util.LinkedHashMap[String, AnyRef]()
                  data.put("name", lb)
                  data.put("attributes", response.loadBalancerAttributes().toBuilder)
                  val resource = toResource(lb, data)
                  cache.put(lb, Attributes(resource))
                  resource
                }
            }
          }
      }
  }
}

object LoadBalancerAttributesCollection {

  private val recentThreshold = Duration.ofMinutes(10).toMillis

  case class Attributes(resource: Resource, timestamp: Long = System.currentTimeMillis()) {
    def isRecent: Boolean = (System.currentTimeMillis() - timestamp) < recentThreshold
    def source: Source[Resource, NotUsed] = Source.single(resource)
  }
}
