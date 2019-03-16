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

import akka.actor.ActorSystem
import com.google.inject.AbstractModule
import com.google.inject.Provides
import com.google.inject.multibindings.Multibinder
import com.netflix.edda.aws.AutoScalingGroupsCollection
import com.netflix.edda.aws.InstancesCollection
import com.netflix.edda.aws.LoadBalancerAttributesCollection
import com.netflix.edda.aws.LoadBalancersCollection
import com.netflix.edda.view.NetflixServerGroupsCollection
import com.netflix.edda.view.ViewInstancesCollection
import com.netflix.iep.aws2.AwsClientFactory
import com.netflix.iep.service.Service
import com.netflix.spectator.api.Registry
import javax.inject.Singleton
import software.amazon.awssdk.services.autoscaling.AutoScalingAsyncClient
import software.amazon.awssdk.services.ec2.Ec2AsyncClient
import software.amazon.awssdk.services.elasticloadbalancing.ElasticLoadBalancingAsyncClient

final class AppModule extends AbstractModule {
  override def configure(): Unit = {
    val serviceBinder = Multibinder.newSetBinder(binder(), classOf[Service])
    serviceBinder.addBinding().to(classOf[CollectionManager])
  }

  @Provides
  @Singleton
  protected def providesAutoScalingClient(factory: AwsClientFactory): AutoScalingAsyncClient = {
    factory.getInstance(classOf[AutoScalingAsyncClient])
  }

  @Provides
  @Singleton
  protected def providesEc2Client(factory: AwsClientFactory): Ec2AsyncClient = {
    factory.getInstance(classOf[Ec2AsyncClient])
  }

  @Provides
  @Singleton
  protected def providesElbClient(factory: AwsClientFactory): ElasticLoadBalancingAsyncClient = {
    factory.getInstance(classOf[ElasticLoadBalancingAsyncClient])
  }

  @Provides
  @Singleton
  protected def providesManager(
    registry: Registry,
    system: ActorSystem,
    ec2: Ec2AsyncClient,
    asg: AutoScalingAsyncClient,
    elb: ElasticLoadBalancingAsyncClient
  ): CollectionManager = {

    val manager = new CollectionManager(registry, system)

    manager
      .add(new InstancesCollection(ec2))
      .add(new AutoScalingGroupsCollection(asg))
      .add(new LoadBalancersCollection(elb))
      .add(new LoadBalancerAttributesCollection(manager, elb))
      .add(new ViewInstancesCollection)
      .add(new NetflixServerGroupsCollection)
  }
}
