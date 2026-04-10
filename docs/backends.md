# Backend Configuration Guide

Fluffy Batch Starter supports three pluggable backend modes. The backend controls how the job queue and coordination (concurrency tracking) are implemented.

## Backend Modes

### H2 (In-Memory) — Default

The default mode. Queue and coordination state live entirely in JVM memory using `ConcurrentLinkedQueue` and `ConcurrentHashMap`. No external infrastructure required.

```yaml
fluffy:
  batch:
    backend:
      type: H2
```

**Characteristics:**
- Zero setup — works out of the box
- Single-node only — state is lost on restart
- Best for development, testing, and single-instance deployments

### Database (PostgreSQL)

Queue entries are stored in a `queue_entry` table and coordination is backed by database queries. Supports multi-node deployments through a shared database.

```yaml
fluffy:
  batch:
    backend:
      type: DATABASE

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/fluffydb
    username: fluffy
    password: secret
  jpa:
    hibernate:
      ddl-auto: update
```

**Characteristics:**
- Persistent queue — survives restarts
- Multi-node safe via shared database
- Enables fault-tolerance recovery (`fluffy.batch.recovery.enabled=true`)
- Moderate throughput (limited by database round-trips)

### Kafka

Job queue is backed by a Kafka topic. Coordination uses the shared database. Best for high-throughput, distributed deployments.

```yaml
fluffy:
  batch:
    backend:
      type: KAFKA
      kafka:
        bootstrap-servers: localhost:9092
        topic: fluffy-jobs
        group-id: fluffy-batch

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/fluffydb
    username: fluffy
    password: secret
```

**Characteristics:**
- High throughput — leverages Kafka consumer groups
- Distributed across multiple nodes
- Requires both Kafka and a shared database
- Enables fault-tolerance recovery

## Property Reference

| Property | Default | Description |
|---|---|---|
| `fluffy.batch.backend.type` | `H2` | Backend mode: `H2`, `DATABASE`, or `KAFKA` |
| `fluffy.batch.backend.kafka.bootstrap-servers` | `localhost:9092` | Kafka broker addresses |
| `fluffy.batch.backend.kafka.topic` | `fluffy-jobs` | Kafka topic for the job queue |
| `fluffy.batch.backend.kafka.group-id` | `fluffy-batch` | Kafka consumer group ID |
| `fluffy.batch.scaling.max-queue-depth` | `100` | Queue depth signal for autoscalers |
| `fluffy.batch.scaling.runtime-updates-enabled` | `true` | Allow runtime config changes via API |
| `fluffy.batch.metrics.enabled` | `true` | Enable Micrometer metrics (requires actuator) |
| `fluffy.batch.recovery.enabled` | `false` | Enable node heartbeat & job recovery |
| `fluffy.batch.recovery.heartbeat-interval` | `15` | Heartbeat interval in seconds |
| `fluffy.batch.recovery.stale-threshold` | `60` | Seconds before a node is considered stale |
| `fluffy.batch.dashboard.enabled` | `true` | Serve the built-in dashboard UI |
| `fluffy.batch.dashboard.path` | `/fluffy-dashboard` | Dashboard base path |
| `fluffy.batch.dashboard.title` | `Fluffy Batch Dashboard` | Dashboard page title |
| `fluffy.batch.dashboard.refresh-interval` | `5` | Dashboard auto-refresh interval in seconds |
| `fluffy.batch.dashboard.auth-enabled` | `false` | Require bearer token for dashboard API |

## When to Use Each Mode

| Criteria | H2 | Database | Kafka |
|---|---|---|---|
| Development / testing | ✅ Best | ✅ OK | ❌ Overkill |
| Single instance, no persistence | ✅ Best | ✅ OK | ❌ Overkill |
| Multi-node with shared state | ❌ | ✅ Best | ✅ Best |
| High-throughput job ingestion | ❌ | ⚠️ Limited | ✅ Best |
| Fault-tolerant recovery | ❌ | ✅ Yes | ✅ Yes |
| Minimal infrastructure | ✅ Best | ⚠️ Needs DB | ❌ Needs DB + Kafka |

## Migration Guide

### H2 → Database

1. Add a PostgreSQL (or other JPA-compatible) datasource to `application.yml`.
2. Set `fluffy.batch.backend.type: DATABASE`.
3. Optionally enable recovery: `fluffy.batch.recovery.enabled: true`.
4. The `queue_entry` and `node_heartbeat` tables are auto-created by Hibernate.

### Database → Kafka

1. Provision a Kafka cluster and create the job topic.
2. Set `fluffy.batch.backend.type: KAFKA`.
3. Configure `fluffy.batch.backend.kafka.*` properties.
4. Keep the shared database — Kafka mode still uses it for coordination and persistence.
5. Deploy multiple instances using the same `group-id` for automatic partition assignment.

### Kafka → Database

1. Set `fluffy.batch.backend.type: DATABASE`.
2. Remove Kafka configuration.
3. Ensure all in-flight Kafka messages are consumed before switching.
