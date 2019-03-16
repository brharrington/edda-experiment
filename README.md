# Edda Experiment

Experiment for rewriting some basic [Edda] functionality using [AWS SDK for Java v2].

[Edda]: https://github.com/Netflix/edda
[AWS SDK for Java v2]: https://github.com/aws/aws-sdk-java-v2

## Building

```
$ project/sbt clean test
```

## Running

First setup [AWS credentials] for the desired account. The region to use can be configured
in the `src/main/resources/application.conf` file. By default `us-east-1` will be used.

[AWS credentials]: https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-credentials.html

Once the credentials are in place you can run using:

```
$ project/sbt run
```

## API

This project provides a limited subset of the [Edda] APIs. Specifically:

### GET /

Returns a list of the initialized collections and the current age of the cached data for that
collection.

### GET /api/${COLLECTION}

Returns a list of ids for the resources in the collection. Optionally the query param `?expand`
can be used to get the full data for all resources in the collection. Currently supported
collections:

| **Path**                        | **Description**                                                                |
|---------------------------------|--------------------------------------------------------------------------------|
| `v2/aws/instances`              | Cached data from [DescribeInstances].                                          |
| `v2/aws/autoScalingGroups`      | Cached data from [DescribeAutoScalingGroups].                                  |
| `v2/aws/loadBalancers`          | Cached data from [DescribeLoadBalancers] (classic ELBs).                       |
| `v2/aws/loadBalancerAttributes` | Cached data from [DescribeLoadBalancerAttributes] for all ELBs.                |
| `v2/view/instances`             | View of instances rather than reservations as returned by [DescribeInstances]. |
| `v2/netflix/serverGroups`       | Netflix view of server groups combining ASG and instance data.                 |

[DescribeInstances]: https://docs.aws.amazon.com/AWSEC2/latest/APIReference/API_DescribeInstances.html
[DescribeAutoScalingGroups]: https://docs.aws.amazon.com/autoscaling/ec2/APIReference/API_DescribeAutoScalingGroups.html
[DescribeLoadBalancers]: https://docs.aws.amazon.com/elasticloadbalancing/2012-06-01/APIReference/API_DescribeLoadBalancers.html
[DescribeLoadBalancerAttributes]: https://docs.aws.amazon.com/elasticloadbalancing/2012-06-01/APIReference/API_DescribeLoadBalancerAttributes.html

### GET /api/${COLLECTION}/${ID}

Returns the full data for a particular resource in the collection.
