# Life of a Job — End-to-End Flow

This document traces a single batch job from the moment a developer writes its
definition through its full lifecycle: submission, queuing, execution, status
transitions, and completion (or failure and retry).

---

## 1. Job Definition

A job starts as a Spring bean that implements `JobHandler` and is annotated
with `@BatchJob`:

```java
@BatchJob(
    name           = "report-generation",
    description    = "Generates a report based on provided parameters",
    async          = true,
    maxConcurrency = 2,
    timeoutSeconds = 360,
    requiredParams = {"reportType"}
)
public class ReportGenerationJob implements JobHandler {

    @Override
    public void execute(JobContext context) throws Exception {
        String reportType = context.requireParam("reportType");
        String dateRange  = context.getParam("dateRange");

        for (int i = 1; i <= 100; i++) {
            context.checkInterrupted();   // honour stop requests
            // ... do work ...
            Thread.sleep(200);
        }
    }
}
```

**Key annotation attributes**

| Attribute | Effect |
|---|---|
| `name` | Unique identifier used in REST URLs (`/api/jobs/{name}/start`) |
| `async` | `true` → job runs on a virtual-thread pool; `false` → blocks the calling thread |
| `maxConcurrency` | Max simultaneous executions of this job; extra requests are queued |
| `timeoutSeconds` | Automatic cancellation if the job runs longer than this |
| `requiredParams` | Parameters that must be present; validated before the job starts |

---

## 2. Registration at Startup

When the application starts, `BatchJobAutoConfiguration` scans for all
Spring beans that implement `JobHandler` and carry `@BatchJob`.  Each is
wrapped in a `JobDefinition` record (an immutable value object) and stored in
the `JobRegistry`.

```
Spring context starts
  └─ BatchJobAutoConfiguration
       └─ Finds all JobHandler beans with @BatchJob
            └─ Builds JobDefinition records
                 └─ Registers each in JobRegistry
```

The `GET /api/jobs/registered` endpoint exposes the registered jobs so the
dashboard and API clients can discover them.

---

## 3. REST Submission

A client submits a job via:

```http
POST /api/jobs/{name}/start
Content-Type: application/json

{
  "parameters": { "reportType": "MONTHLY", "dateRange": "2025-01" },
  "requestedBy": "alice"
}
```

`JobController` receives the request and delegates to `JobLauncher.launch()`.

---

## 4. Validation

Before any execution record is created, `JobLauncher` checks that all
`requiredParams` are present in the request.  If any are missing, it throws
`IllegalArgumentException`, which `GlobalExceptionHandler` converts to an HTTP
400 response with a `ProblemDetail` body:

```json
{
  "type":   "about:blank",
  "title":  "Bad Request",
  "status": 400,
  "detail": "Missing required parameter: reportType"
}
```

---

## 5. Execution Record Creation — Status: `STARTING`

Once validation passes, `JobLauncher` persists a `JobExecution` row with
`status = STARTING`:

```
JobExecution {
  id          : 42
  jobName     : "report-generation"
  status      : STARTING
  requestedBy : "alice"
  startTime   : 2025-01-15T09:00:00Z
  parameters  : {"reportType":"MONTHLY","dateRange":"2025-01"}
}
```

The execution ID (`42`) is returned to the caller immediately via HTTP 202.

---

## 6. Concurrency Check

`JobLauncher` checks whether the job can run right now:

```
CoordinationBackend.canRun("report-generation", maxConcurrency=2)
```

- **H2 backend** — in-memory atomic counter.
- **Database backend** — a database row per job name with a pessimistic count.
- **Kafka backend** — database coordination with Kafka queue.

### Path A — Slot available → Execute

`coordinationBackend.increment("report-generation")` reserves a slot, the
status becomes `STARTED`, and the job runs (async or sync depending on the
annotation).

### Path B — No slot → Queue

The execution ID is pushed onto the `QueueBackend` and the `queuePosition`
field is set on the execution record.  The caller still gets the execution ID
and can poll for status.

---

## 7. Execution — Status: `STARTED`

For **async** jobs (the common case), `JobLauncher` submits the handler to a
virtual-thread executor:

```
executorService.submit(() -> executeJob(executionId, def, context))
```

Java 21 virtual threads let many jobs run concurrently with minimal OS-thread
overhead — ideal for I/O-bound work (HTTP calls, database queries, file
operations).

If `timeoutSeconds > 0`, a `ScheduledExecutorService` fires after the deadline,
marks the `JobContext` as stop-requested, and cancels the virtual-thread future.

Inside `executeJob`:

```
1. Update status → STARTED
2. Call handler.execute(context)
3. On success  → status = COMPLETED
4. On InterruptedException → status = STOPPED
5. On any other exception  → status = FAILED (error message saved)
6. Always:
     - Remove from runningContexts / runningFutures
     - coordinationBackend.decrement(jobName)
     - Set endTime
     - processQueue(jobName)   ← promote next queued execution if slot freed
```

---

## 8. Cooperative Stop with `checkInterrupted()`

Your job handler should call `context.checkInterrupted()` inside any loop or
before expensive operations:

```java
for (int i = 0; i < totalItems; i++) {
    context.checkInterrupted();   // throws InterruptedException if stop requested
    processItem(i);
}
```

When a client calls `POST /api/jobs/executions/{id}/stop`:

1. `JobLauncher.stop()` marks the `JobContext` with `stopRequested = true`.
2. The next `checkInterrupted()` call throws `InterruptedException`.
3. `executeJob` catches it and sets `status = STOPPED`.

---

## 9. Queue Promotion

When a job finishes (any terminal status), `processQueue` runs:

```
while (canRun(jobName, maxConcurrency)) {
    nextId = queueBackend.poll()
    if nextId == null → break
    context = runningContexts.get(nextId)
    increment(jobName)
    update nextId → STARTED
    run nextId
}
```

This ensures queued jobs are started as quickly as possible after a slot opens,
respecting `maxConcurrency` at all times.

---

## 10. Status Transitions — Full Diagram

```
                          ┌────────────┐
                          │  STARTING  │  ← persisted immediately on submit
                          └─────┬──────┘
              slot available    │    no slot available
                  ┌─────────────┼─────────────┐
                  ▼                           ▼
           ┌──────────┐               ┌───────────┐
           │  STARTED │◀──promoted────│  STARTING │ (queue position > 0)
           └─────┬────┘               └───────────┘
                 │
      ┌──────────┼──────────────────┐
      ▼          ▼                  ▼
 ┌─────────┐ ┌───────┐        ┌─────────┐
 │COMPLETED│ │STOPPED│        │  FAILED │
 └─────────┘ └───────┘        └─────────┘
                                    │
                                    ▼ (retry)
                               STARTING (new execution)
```

| Status | Meaning |
|---|---|
| `STARTING` | Persisted, waiting for a concurrency slot |
| `STARTED` | Running on a virtual thread |
| `COMPLETED` | Handler returned normally |
| `STOPPED` | Cancelled by client or timeout |
| `FAILED` | Handler threw an exception |

---

## 11. Polling for Status

Clients can poll at any time:

```bash
# Single execution
curl http://localhost:8080/api/jobs/executions/42

# All executions (pageable, filterable)
curl "http://localhost:8080/api/jobs/executions?status=STARTED"
```

The dashboard polls automatically and renders live updates.

---

## 12. Retry

A FAILED or STOPPED execution can be retried:

```bash
curl -X POST http://localhost:8080/api/jobs/executions/42/retry
```

`JobLauncher.retry()`:
1. Loads the original execution.
2. Rejects the retry if status is `STARTING` or `STARTED` (job is still running).
3. Creates a **new** `JobRequest` from the original parameters.
4. Calls `launch()` — producing a new execution ID.

The original execution record is preserved; the retry produces a distinct row.

---

## 13. Recovery (Optional)

When `fluffy.batch.recovery.enabled=true`, `RecoveryManager` runs at startup
and scans for executions stuck in `STARTING` or `STARTED`.  These are
executions from a previous instance that crashed before updating the status.
Each is marked `FAILED` with the error message `"Recovered from crash"` and
re-queued automatically.

See [Fault Tolerance & Recovery](recovery.md) for configuration details.

---

## 14. Metrics

Throughout the lifecycle, Micrometer gauges and counters are updated:

| Metric | When updated |
|---|---|
| `fluffy.jobs.queued` | On enqueue / dequeue |
| `fluffy.jobs.running` | On STARTED / terminal status |
| `fluffy.jobs.completed.total` | On COMPLETED |
| `fluffy.jobs.failed.total` | On FAILED |

Expose them via Prometheus at `/actuator/prometheus`.

See [Metrics](metrics.md) for Grafana dashboard and KEDA autoscaling examples.
