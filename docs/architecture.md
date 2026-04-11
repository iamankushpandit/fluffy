# Architecture

## Overview

Fluffy Batch Starter is a Spring Boot auto-configuration library that provides a batch job execution framework via REST APIs. It targets **Java 21+** and **Spring Boot 3.4+**, leveraging modern language features and runtime capabilities.

The architecture is built around **pluggable backends** — the queue and coordination layers are abstracted behind interfaces so you can run with an in-memory H2 backend, a shared Database backend, or a distributed Kafka backend without changing application code.

## Java 21 & Spring Boot 3.4 Features

- **Records** — `JobRequest`, `JobStatusResponse`, and `JobDefinition` are immutable Java records, reducing boilerplate and improving thread safety.
- **Virtual Threads** — The job executor uses `Executors.newVirtualThreadPerTaskExecutor()`, enabling lightweight, high-throughput concurrency ideal for I/O-bound batch jobs.
- **`java.time.Instant`** — All timestamps use `Instant` instead of the legacy `java.sql.Timestamp`.
- **RFC 7807 Problem Details** — Error responses follow the `ProblemDetail` standard for consistent, machine-readable error payloads.

## Pluggable Backend Architecture

All queue and coordination operations are defined by two interfaces:

### QueueBackend

Abstracts the job queue. Implementations:

| Method | Description |
|---|---|
| `enqueue(executionId)` | Add a job to the queue |
| `poll()` | Retrieve the next queued job |
| `remove(executionId)` | Remove a specific job from the queue |
| `getPosition(executionId)` | Return a job's queue position |
| `size()` | Current queue length |
| `contains(executionId)` | Check if a job is queued |

### CoordinationBackend

Abstracts concurrency tracking. Implementations:

| Method | Description |
|---|---|
| `canRun(jobName, maxConcurrency)` | Check if a job can start |
| `increment(jobName)` | Record a job starting |
| `decrement(jobName)` | Record a job finishing |
| `getRunningCount(jobName)` | Running count for a specific job |
| `getGlobalRunningCount()` | Total running jobs |

### Backend Modes

| Mode | QueueBackend | CoordinationBackend | Use Case |
|---|---|---|---|
| **H2** (default) | In-memory `ConcurrentLinkedQueue` | In-memory `ConcurrentHashMap` | Dev, testing, single node |
| **Database** | `queue_entry` table (JPA) | Database queries | Multi-node with shared DB |
| **Kafka** | Kafka topic consumer | Database queries | High-throughput distributed |

Set the mode with `fluffy.batch.backend.type`: `H2`, `DATABASE`, or `KAFKA`. See [Backend Configuration Guide](backends.md) for details.

## Components

### Core Engine
- **JobRegistry**: Stores job definitions. Auto-discovers `@BatchJob` beans.
- **JobLauncher**: Orchestrates job lifecycle (launch, stop, retry). Delegates to `QueueBackend` and `CoordinationBackend`.
- **ConcurrencyManager**: Delegates to the active `CoordinationBackend` to track running job counts.
- **JobQueueManager**: Delegates to the active `QueueBackend` for pending jobs.
- **RecoveryManager**: Heartbeat-based stale-node detection and job recovery (Database/Kafka modes).
- **FluffyBatchMetrics**: Micrometer gauges and counters for queue depth, active jobs, and completion rates.

### Persistence
- **JobExecution**: JPA entity storing execution state, including `ownerNode` for multi-node tracking.
- **JobExecutionRepository**: Spring Data JPA repository.
- **QueueEntry**: JPA entity for the database-backed queue.
- **NodeHeartbeat**: JPA entity for node liveness tracking.

### Web Layer
- **JobController**: REST endpoints for job management.
- **ScalingConfigController**: REST endpoints for runtime scaling configuration.
- **DashboardConfigController**: Dashboard configuration endpoint.
- **GlobalExceptionHandler**: Unified error handling using `ProblemDetail`.

### Auto-Configuration
- **BatchJobAutoConfiguration**: Registers core beans, sets up JPA, and configures virtual-thread-based executors.
- **DatabaseBackendAutoConfiguration**: Activates when `fluffy.batch.backend.type=DATABASE`.
- **KafkaBackendAutoConfiguration**: Activates when `fluffy.batch.backend.type=KAFKA`.
- **MetricsAutoConfiguration**: Registers Micrometer metrics when actuator is present.
- **RecoveryAutoConfiguration**: Enables heartbeat and recovery when configured.
- **ScalingAutoConfiguration**: Registers scaling properties and controller.

## Component Interaction

```
┌─────────────┐     ┌──────────────┐     ┌───────────────────┐
│ JobController│────▶│  JobLauncher  │────▶│   QueueBackend    │
│ (REST API)  │     │               │     │ (H2/DB/Kafka)     │
└─────────────┘     │               │     └───────────────────┘
                    │               │────▶┌───────────────────┐
                    │               │     │CoordinationBackend│
                    └──────────────┘     │ (H2/DB)           │
                           │              └───────────────────┘
                           ▼
                    ┌──────────────┐
                    │ JobExecution  │
                    │ (JPA Entity)  │
                    └──────────────┘
                           │
                           ▼
                    ┌──────────────┐
                    │RecoveryManager│ (Database/Kafka only)
                    │  Heartbeat   │
                    └──────────────┘
```

## Flow

1. POST /api/jobs/{name}/start → JobController → JobLauncher.launch()
2. JobLauncher validates params, creates JobExecution (IN_QUEUE)
3. Job is enqueued via the active `QueueBackend`
4. If concurrency allows (checked via `CoordinationBackend`): STARTED → IN_PROGRESS → SUCCESS/FAILURE
5. If concurrency full: stays IN_QUEUE in the queue backend
6. When a job finishes: processQueue() polls the queue backend for the next pending job
7. In Database/Kafka modes, `RecoveryManager` periodically checks for stale nodes and re-queues orphaned jobs
