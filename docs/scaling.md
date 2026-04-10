# Scaling Configuration

Fluffy Batch Starter provides scaling signals and a runtime API to integrate with Kubernetes autoscalers.

## ScalingProperties Reference

| Property | Default | Description |
|---|---|---|
| `fluffy.batch.scaling.max-queue-depth` | `100` | Maximum queue depth signal exposed to autoscalers |
| `fluffy.batch.scaling.runtime-updates-enabled` | `true` | Allow updating scaling config via REST API |

```yaml
fluffy:
  batch:
    scaling:
      max-queue-depth: 100
      runtime-updates-enabled: true
```

## Runtime Configuration API

### Get Current Config

```
GET /api/jobs/config/scaling
```

Response:

```json
{
  "maxQueueDepth": 100
}
```

### Update Config

```
POST /api/jobs/config/scaling
Content-Type: application/json

{
  "maxQueueDepth": 200
}
```

Response:

```json
{
  "maxQueueDepth": 200
}
```

Runtime updates take effect immediately. Disable them in production by setting `fluffy.batch.scaling.runtime-updates-enabled: false`.

## Kubernetes HPA Configuration

Use the Prometheus Adapter to expose `fluffy_batch_queue_depth` as a custom metric, then create an HPA:

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: fluffy-batch-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: fluffy-batch
  minReplicas: 1
  maxReplicas: 10
  metrics:
    - type: Pods
      pods:
        metric:
          name: fluffy_batch_queue_depth
        target:
          type: AverageValue
          averageValue: "20"
```

### Prometheus Adapter Rule

```yaml
rules:
  - seriesQuery: 'fluffy_batch_queue_depth'
    resources:
      overrides:
        namespace: { resource: "namespace" }
        pod: { resource: "pod" }
    name:
      matches: "^(.*)$"
      as: "${1}"
    metricsQuery: 'avg(<<.Series>>{<<.LabelMatchers>>}) by (<<.GroupBy>>)'
```

## KEDA Integration

[KEDA](https://keda.sh/) provides event-driven autoscaling. Use the Prometheus scaler to scale based on queue depth:

```yaml
apiVersion: keda.sh/v1alpha1
kind: ScaledObject
metadata:
  name: fluffy-batch-scaledobject
spec:
  scaleTargetRef:
    name: fluffy-batch
  minReplicaCount: 1
  maxReplicaCount: 10
  triggers:
    - type: prometheus
      metadata:
        serverAddress: http://prometheus.monitoring:9090
        metricName: fluffy_batch_queue_depth
        query: avg(fluffy_batch_queue_depth)
        threshold: "20"
```

For Kafka-backed deployments, you can also use the KEDA Kafka trigger to scale based on consumer lag:

```yaml
triggers:
  - type: kafka
    metadata:
      bootstrapServers: kafka.default:9092
      consumerGroup: fluffy-batch
      topic: fluffy-jobs
      lagThreshold: "10"
```

This scales pods when the Kafka consumer group falls behind, complementing the queue-depth-based scaling.
