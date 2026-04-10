# Batch Job Execution Starter – Full Design & Production Readiness Specification

---

## 1. Overview

This project is a **Spring Boot starter (library, not an application)** that enables any Spring Boot application to execute long-running or background jobs via REST APIs.

### Core Principle

> The consumer writes only business logic. The framework handles execution, lifecycle, concurrency, and infrastructure.

---

## 2. Core Capabilities

- Async and sync job execution
- Auto-registered REST endpoints
- Job lifecycle management
- Simplified status model
- Concurrency control with queueing
- Retry and stop mechanisms
- Parameter validation
- User identification
- Optional dashboard UI
- Zero-config default setup

---

## 3. Architecture Overview

The system consists of four primary layers:

### 3.1 Developer API Layer

- Annotation-based jobs
- Fluent builder jobs
- Interface-based jobs
- Job context abstraction

### 3.2 Execution Engine

- Job registry
- Job launcher (sync + async)
- Concurrency manager
- Lifecycle handler

### 3.3 Infrastructure Layer

- Metadata persistence
- Queue persistence (initially in-memory)
- DataSource isolation

### 3.4 Spring Boot Integration

- Auto-configuration
- REST controllers
- Conditional beans
- Optional dashboard

---

## 4. Job Definition Model

### Supported Styles

#### Annotation-based

- Declarative
- Spring-native

#### Builder-based

- Fluent API
- Explicit configuration

#### Interface-based

- Full control

### Constraint

All styles must resolve to a unified internal model:

```java
JobDefinition
```

---

## 5. Job Lifecycle & Status Model

### Status Model

```
IN_QUEUE
STARTED
IN_PROGRESS
SUCCESS
FAILURE
STOPPED
```

### Rules

- Queue is distinct from execution
- STOPPED is not FAILURE
- Framework-specific statuses must not leak

---

## 6. REST API

Base path:

```
/api/jobs
```

Endpoints:

- Start job
- Get status
- Stop job
- Retry job
- List executions
- List registered jobs

---

## 7. Data Contracts

### Request

```java
JobRequest {
  Map parameters
  String arguments
}
```

### Response

```java
JobStatusResponse {
  Long jobId
  String jobName
  String status
  String requestedBy
  Timestamp startTime
  Timestamp endTime
  Integer queuePosition
}
```

---

## 8. Job Context

Provides:

- execution ID
- user
- parameters (immutable)
- helper methods:
  - getParam
  - requireParam
  - checkInterrupted

---

## 9. Concurrency & Queueing

### Features

- Per-job concurrency limit
- Global concurrency limit
- FIFO queue
- Queue position tracking

### Implementation (Current)

- In-memory queue
- Atomic counters

---

## 10. Stop & Timeout

### Stop

- Removes from queue OR interrupts execution
- Marks job as STOPPED

### Timeout

- Watchdog thread
- Interrupts job after duration

---

## 11. Retry

- Reuses parameters
- Creates new execution ID
- Preserves async/sync behavior

---

## 12. Observability (Baseline)

- Status tracking
- Execution metadata
- Queue audit table

---

## 13. Security (Baseline)

- User extracted from request
- Basic token handling for dashboard

---

## 14. Testing

- Unit tests for:
  - lifecycle
  - validation
  - concurrency
- Integration tests for:
  - REST APIs
  - queue behavior

---

## 15. Supporting Deliverables

### Example Application

- Demonstrates all job types
- Covers:
  - async/sync
  - retry
  - stop
  - queueing

### API Collection

- Postman collection
- Covers all endpoints

### README

Includes documentation tree:

```
docs/
  architecture.md
  job-definition.md
  rest-api.md
  concurrency.md
  retry-stop.md
  datasource.md
  dashboard.md
```

---

## 16. Production Gaps & Cloud Limitations

### 16.1 Single-Instance Assumption

**Problem:**

- In-memory queue
- Local thread tracking
- No shared state

**Impact:**

- Breaks in multi-instance deployments
- Jobs split across nodes unpredictably

### 16.2 No Distributed Queue

**Problem:**

- Queue not durable
- Lost on restart

**Impact:**

- Jobs lost or duplicated

### 16.3 Local Concurrency Only

**Problem:**

- Counters are in-memory

**Impact:**

- Cannot enforce global limits across instances

### 16.4 No Worker Ownership

**Problem:**

- No tracking of which node runs a job

**Impact:**

- Cannot recover jobs after crash

### 16.5 Stop Is Not Reliable

**Problem:**

- Relies on thread interruption

**Impact:**

- May fail in blocking operations or cross-node cases

### 16.6 No Idempotency

**Problem:**

- Duplicate requests create duplicate jobs

### 16.7 Weak Observability

Missing:

- metrics
- tracing
- structured logs

### 16.8 Security Limitation

- No built-in authentication or authorization
- Relies on consumer-provided security configuration

---

<!-- Sections 17–30 were not available for transcription from the provided screenshots. -->

---

## 31. Deep-Dive Documentation

The following deep-dive documents should cover:

- Concurrency & queueing internals (CAS loops, negative tickets)
- Kill switch internals (thread tracking, InterruptedException handling)
- Timeout watchdog internals (virtual threads, cancellation)
- Status model mapping and refinement logic
- Dashboard architecture (static SPA, apiFetch wrapper, token handling)
- Dashboard config endpoints
- Security model (3 scenarios)
- Design decisions & trade-offs (at least 8 documented decisions with rationale)

### DAY-IN-THE-LIFE-DEVELOPER.md

Walkthrough from a developer's perspective: scenario → choose style → write job → write test → start app → test endpoints → test dashboard → test safety features → commit.

### DAY-IN-THE-LIFE-JOB.md

Walkthrough from a job's perspective: startup (zombie reconciliation) → request arrives → controller validates → concurrency gate → build Spring Batch artifacts → launch → execution → status polling → completion → callbacks → alternate endings (failure, stop, timeout, queued, queue full). Complete timeline with timestamps.

---

## 32. Design Constraints & Decisions

These are **explicit design choices** that must be preserved:

1. **Single-tasklet jobs only** – no multi-step orchestration. Each job = one tasklet. Consumers needing steps should use Spring Batch directly.

2. **Dynamic Job/Step/Tasklet creation** – create a new `Job` → `Step` → `Tasklet` for every launch, not pre-registered beans. This allows injecting per-execution context into the closure.

3. **Unique run.id via epoch milliseconds** – `System.currentTimeMillis()` as the identifying `run.id` parameter. Guarantees unique `BATCH_JOB_INSTANCE` rows even across multiple applications sharing the same DB.

4. **Negative ticket IDs for queued jobs** – queued entries are not Spring Batch executions. They get negative IDs (decrementing from -1) to avoid collision with real execution IDs.

5. **Two separate JobLaunchers** – one async (virtual thread executor), one sync (caller thread). Routed based on `job.isSync()`.

6. **In-memory queueing** – queued jobs live in `ConcurrentLinkedQueue` in the JVM. Lost on restart. Acceptable for single-instance deployments. Queue audit table provides a record.

7. **`@ConditionalOnMissingBean` on every starter bean** – consumer can override anything.

8. **Reflection-based annotation adapter** – `@BatchJob` classes are POJOs. The adapter calls `@Execute` via `Method.invoke()`. Startup-time validation ensures `@Execute` exists.

9. **`InterruptedException` → STOPPED, not FAILED** – the tasklet catches `InterruptedException`, sets `ExitStatus.STOPPED`, and returns `FINISHED`. Spring Batch sees: normal return + `terminateOnly` → status = STOPPED.
