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
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.netflix.edda.Resource
import com.netflix.edda.ResourceMap
import com.netflix.edda.ViewCollection
import com.netflix.spectator.ipc.ServerGroup

import scala.collection.JavaConverters._
import scala.collection.SortedMap

/**
  * Unified view of server groups combining data from both DescribeInstances and
  * DescribeAutoScalingGroups. Includes fields to break out the parts of a server group
  * name based on the Frigga conventions used by Spinnaker.
  */
class NetflixServerGroupsCollection extends ViewCollection {

  private val mapper = new ObjectMapper()

  override def path: String = "v2/netflix/serverGroups"

  override def dependencies: Set[String] = Set(
    "v2/view/instances",
    "v2/aws/autoScalingGroups"
  )

  private def put(obj: ObjectNode, field: String, value: String): Unit = {
    if (value != null && !value.isEmpty) {
      obj.put(field, value)
    }
  }

  private def isHealthy(asgState: JsonNode, ec2State: JsonNode): Boolean = {
    val asgLifecycle = asgState.get("lifecycleStateAsString").textValue() == "InService"
    val asgHealth = asgState.get("healthStatus").textValue() == "Healthy"
    val ec2 = ec2State.get("state").get("nameAsString").textValue() == "running"
    asgLifecycle && asgHealth && ec2
  }

  private def toServerGroup(asg: Resource, instances: ResourceMap): Resource = {
    val id = s"ec2.${asg.id}"
    val group = ServerGroup.parse(asg.id)

    val objectNode = mapper.createObjectNode()
    put(objectNode, "id", id)
    put(objectNode, "platform", "ec2")
    put(objectNode, "app", group.app())
    put(objectNode, "cluster", group.cluster())
    put(objectNode, "group", asg.id)
    put(objectNode, "stack", group.stack())
    put(objectNode, "detail", group.detail())
    objectNode.put("minSize", asg.data.get("minSize").asInt(0))
    objectNode.put("maxSize", asg.data.get("maxSize").asInt(0))
    objectNode.put("desiredSize", asg.data.get("desiredCapacity").asInt(0))

    val instancesArray = mapper.createArrayNode()
    asg.data.get("instances").elements().asScala.foreach { asgData =>
        val instanceId = asgData.get("instanceId").textValue()
        instances.get(instanceId)
          .filter(r => isHealthy(asgData, r.data))
          .foreach { r =>
            val instanceNode = mapper.createObjectNode()
            instanceNode.put("node", instanceId)
            instanceNode.put("zone", asgData.get("availabilityZone").textValue())

            val ec2Data = r.data
            instanceNode.put("vpcId", ec2Data.get("vpcId").textValue())
            instanceNode.put("subnetId", ec2Data.get("subnetId").textValue())
            instanceNode.put("ami", ec2Data.get("imageId").textValue())
            instanceNode.put("vmtype", ec2Data.get("instanceTypeAsString").textValue())
            instanceNode.put("privateIpAddress", ec2Data.get("privateIpAddress").textValue())
            instanceNode.put("launchTime", ec2Data.get("launchTime").get("epochSecond").asLong(0) * 1000)

            instancesArray.add(instanceNode)
          }
    }
    objectNode.set("instances", instancesArray)

    Resource(id, objectNode)
  }

  override def materialize(data: Map[String, ResourceMap]): ResourceMap = {
    val instances = data("v2/view/instances")
    val groups = data("v2/aws/autoScalingGroups")
    val resources = groups.values.map { asg => toServerGroup(asg, instances) }
    SortedMap.empty[String, Resource] ++ resources.map(r => r.id -> r)
  }
}
