# Metrics Reference

Fluffy Batch Starter exposes Micrometer metrics when Spring Boot Actuator is on the classpath.

## Configuration

```yaml
fluffy:
  batch:
    metrics:
      enabled: true  # default when actuator is present

management:
  endpoints:
    web:
      exposure:
        include: prometheus,health
  metrics:
    export:
      prometheus:
        enabled: true
```

Set `fluffy.batch.metrics.enabled: false` to disable all Fluffy metrics.

## Exposed Metrics

| Metric | Type | Description |
|---|---|---|
| `fluffy.batch.queue.depth` | Gauge | Current number of jobs waiting in the queue |
| `fluffy.batch.jobs.active` | Gauge | Number of jobs currently running |
| `fluffy.batch.jobs.completed.total` | Counter | Total jobs completed successfully |
| `fluffy.batch.jobs.failed.total` | Counter | Total jobs that ended in failure |
| `fluffy.batch.jobs.stopped.total` | Counter | Total jobs stopped by request |

## Prometheus Endpoint

With the configuration above, metrics are available at:

```
GET /actuator/prometheus
```

Example output:

```
# HELP fluffy_batch_queue_depth Current queue depth
# TYPE fluffy_batch_queue_depth gauge
fluffy_batch_queue_depth 3.0

# HELP fluffy_batch_jobs_active Currently running jobs
# TYPE fluffy_batch_jobs_active gauge
fluffy_batch_jobs_active 2.0

# HELP fluffy_batch_jobs_completed_total Total completed jobs
# TYPE fluffy_batch_jobs_completed_total counter
fluffy_batch_jobs_completed_total 154.0

# HELP fluffy_batch_jobs_failed_total Total failed jobs
# TYPE fluffy_batch_jobs_failed_total counter
fluffy_batch_jobs_failed_total 7.0

# HELP fluffy_batch_jobs_stopped_total Total stopped jobs
# TYPE fluffy_batch_jobs_stopped_total counter
fluffy_batch_jobs_stopped_total 2.0
```

## Grafana Dashboard Queries

### Queue Depth Over Time

```promql
fluffy_batch_queue_depth
```

### Active Jobs

```promql
fluffy_batch_jobs_active
```

### Job Completion Rate (per minute)

```promql
rate(fluffy_batch_jobs_completed_total[1m])
```

### Job Failure Rate (per minute)

```promql
rate(fluffy_batch_jobs_failed_total[1m])
```

### Failure Ratio

```promql
rate(fluffy_batch_jobs_failed_total[5m])
  / (rate(fluffy_batch_jobs_completed_total[5m]) + rate(fluffy_batch_jobs_failed_total[5m]))
```

### Suggested Alert: High Queue Depth

```yaml
- alert: FluffyQueueBacklog
  expr: fluffy_batch_queue_depth > 50
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "Fluffy job queue depth is {{ $value }}"
```

### Suggested Alert: Elevated Failure Rate

```yaml
- alert: FluffyHighFailureRate
  expr: rate(fluffy_batch_jobs_failed_total[5m]) > 0.1
  for: 5m
  labels:
    severity: critical
  annotations:
    summary: "Fluffy job failure rate is {{ $value }}/s"
```
