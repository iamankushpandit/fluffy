# Fluffy — Industry Use Cases & Developer Advertisement

> **Before you build yet another batch framework from scratch, read this.**

---

## Why Fluffy Exists

Every growing application eventually needs batch processing — nightly reports,
data migrations, bulk notifications, compliance exports. Teams routinely spend
**weeks or months** building custom queue-and-execute infrastructure that Fluffy
delivers out of the box in a single Maven dependency.

Fluffy is a lightweight, annotation-driven Spring Boot starter for batch job
processing. It provides a REST API to launch, stop, retry, and monitor jobs
with built-in concurrency management, pluggable backends (H2 / PostgreSQL /
Kafka), fault-tolerant recovery, Kubernetes-native autoscaling, and a real-time
dashboard — all powered by Java 21 virtual threads.

---

## Industry Use Cases

### 🏥 Healthcare

Healthcare systems handle sensitive patient data under strict regulatory
requirements (HIPAA, HL7, GDPR). Before building a new batch layer, consider
whether Fluffy already covers the need.

| Use Case | What You'd Build Without Fluffy | What Fluffy Gives You |
|---|---|---|
| **EHR data synchronization** — sync patient records between hospital systems on a nightly or on-demand basis | Custom thread pool, retry logic, error tracking, audit trail | `@BatchJob` with `maxConcurrency`, automatic retry, per-execution status history via REST |
| **Insurance claim processing** — validate, enrich, and submit batches of insurance claims to payers | Queue infrastructure, concurrency limiter, dead-letter handling | Pluggable queue backend (Database or Kafka), per-job concurrency control, stop/retry endpoints |
| **Lab result report generation** — generate PDF/HL7 reports for thousands of lab results after a test run completes | Worker pool, progress tracking, failure recovery | Virtual-thread execution for I/O-bound PDF generation, dashboard for real-time progress, fault-tolerant recovery across nodes |
| **Patient data export (HIPAA / GDPR right-of-access)** — export a patient's complete record on request | Request queue, background worker, status polling endpoint | REST trigger (`POST /api/jobs/patient-export/start`), automatic queuing, status endpoint for the requesting system |
| **Medical device telemetry ingestion** — ingest and process bulk uploads from connected medical devices | Kafka consumer boilerplate, offset management, scaling logic | Kafka backend mode with KEDA autoscaling — scale pods based on consumer lag |
| **Appointment reminder dispatch** — send SMS/email reminders to thousands of patients daily | Scheduler + worker + rate limiter | `maxConcurrency` to rate-limit outbound API calls, queue overflow handled automatically |
| **Clinical trial data aggregation** — aggregate multi-site trial data into a central data warehouse | ETL orchestrator, failure handling, node coordination | Multi-node Database backend with shared queue and coordination, heartbeat-based recovery for long-running aggregations |

**Bottom line:** Healthcare batch workloads need auditability, fault tolerance,
and compliance-friendly traceability. Fluffy's per-execution tracking, REST
status API, and recovery mechanism deliver these without custom code.

---

### 🏦 Financial Services

Financial systems demand precision, auditability, and resilience. Batch
processing underpins settlement, reconciliation, regulatory reporting, and
fraud detection.

| Use Case | What You'd Build Without Fluffy | What Fluffy Gives You |
|---|---|---|
| **End-of-day settlement** — process all day's transactions into settlement batches for clearing houses | Distributed lock, ordered queue, failure recovery, multi-node coordination | Database or Kafka backend for shared coordination, `maxConcurrency = 1` to ensure sequential settlement, recovery re-queues if a node crashes mid-settlement |
| **Regulatory report generation (SOX, Basel III, MiFID II)** — produce compliance reports on a schedule | Custom report runner, parameter validation, audit log | `requiredParams` annotation for mandatory report parameters, full execution history via REST API, RFC 7807 error responses for failed validations |
| **Transaction reconciliation** — reconcile bank transactions with partner ledgers nightly | Batch runner with retry, partial-failure handling | Stop/retry API lets operations pause and resume reconciliation, queue position tracking shows backlog size |
| **Fraud detection batch screening** — score transactions against fraud models in bulk | Thread pool tuning, back-pressure, scaling | Virtual threads handle thousands of concurrent HTTP calls to scoring services; Kubernetes HPA/KEDA autoscaling adjusts pod count based on queue depth |
| **Account statement generation** — generate monthly statements for millions of accounts | Massive parallel runner, progress monitoring | Scale horizontally with Database backend, monitor progress via the aggregator dashboard across all nodes |
| **Portfolio risk recalculation** — recompute risk metrics (VaR, stress tests) across portfolios after market close | Compute-intensive job management, timeout handling | `timeoutSeconds` prevents runaway calculations, `maxConcurrency` limits CPU-intensive jobs to avoid cluster resource exhaustion |
| **KYC/AML batch verification** — verify customer identities against external watchlists in bulk | API call orchestration, rate limiting, failure tracking | `maxConcurrency` acts as a natural rate limiter for external API calls, automatic retry on transient failures |

**Bottom line:** Financial batch workloads require zero data loss, audit
trails, and the ability to halt and resume operations. Fluffy's stop/retry
endpoints, database-backed persistence, and heartbeat recovery provide these
guarantees without a custom framework.

---

### 📱 Social Media

Social media platforms process enormous volumes of user-generated content,
notifications, and analytics. Scale is unpredictable — a viral post can spike
load 100×.

| Use Case | What You'd Build Without Fluffy | What Fluffy Gives You |
|---|---|---|
| **Content moderation queue** — process flagged posts/images/videos through AI moderation models | Worker pool, priority queue, rate limiter for model API | `maxConcurrency` controls how many items hit the moderation API simultaneously, queue ensures nothing is dropped |
| **Notification fan-out** — push notifications to millions of users when a popular creator posts | Kafka consumer, partitioning strategy, scaling infrastructure | Kafka backend mode handles high-throughput ingestion; KEDA scales pods based on consumer lag during viral spikes |
| **User data export (GDPR / CCPA)** — fulfill "download my data" requests within the legal deadline | Request queue, background worker, compliance tracking | Each export is a tracked execution with status history — perfect for compliance audit trails |
| **Analytics roll-up** — aggregate engagement metrics (likes, shares, views) across shards hourly | MapReduce or custom aggregation, failure handling | Multi-node Database backend ensures each shard is processed exactly once; dashboard shows real-time roll-up progress |
| **Trending topic computation** — compute trending scores across regions periodically | Distributed computation framework | Fluffy's scaling API lets you dynamically adjust `maxQueueDepth` during peak events (elections, sports finals) so autoscaling responds in real time |
| **Creator payout calculation** — compute ad-revenue payouts for creators monthly | Financial-grade batch runner, parameter validation, audit log | `requiredParams` ensures every payout job specifies the billing period; per-execution tracking provides a complete audit trail |
| **Spam/bot detection sweep** — batch-analyze user accounts for spam or bot behavior | Worker infrastructure, progress tracking, restart logic | Virtual threads efficiently process I/O-bound API calls to detection services; recovery re-queues sweeps if a node restarts during a scan |
| **Media transcoding pipeline** — transcode uploaded videos into multiple formats and resolutions | Job queue, progress tracking, failure/retry | Launch a transcoding job per upload via REST, track progress per execution, retry failed transcodes with one API call |

**Bottom line:** Social media workloads are bursty and massive. Fluffy's Kafka
backend, Kubernetes autoscaling, and virtual-thread concurrency give you
elastic batch processing that scales with viral moments — without maintaining
a homegrown orchestration layer.

---

## Full Advertisement Text

---

# 🐾 Fluffy Batch Starter

## **The batch framework that ships so you don't have to build one.**

---

### The Problem

You need batch processing. Every team does eventually.

Report generation. Data sync. Bulk notifications. Compliance exports. File
processing. Account reconciliation. Content moderation.

So you start building. A thread pool here. A queue there. Retry logic. Status
tracking. A dashboard. Multi-node coordination. Failure recovery.

**Three sprints later, you're maintaining infrastructure instead of shipping
features.**

---

### The Solution

**Fluffy is a single Maven dependency that gives you a production-grade batch
job framework in minutes — not months.**

```xml
<dependency>
  <groupId>com.fluffy</groupId>
  <artifactId>fluffy-batch-starter</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Annotate a class. Hit a REST endpoint. Your job is running, tracked, retryable,
and visible on a dashboard.

```java
@BatchJob(name = "nightly-report", maxConcurrency = 3, async = true)
public class NightlyReportJob implements JobHandler {

    @Override
    public void execute(JobContext context) throws Exception {
        String region = context.requireParam("region");
        // your business logic — that's it
    }
}
```

```bash
curl -X POST http://localhost:8080/api/jobs/nightly-report/start \
     -H "Content-Type: application/json" \
     -d '{"parameters": {"region": "US-EAST"}, "requestedBy": "scheduler"}'
```

**Done.** Fluffy handles queuing, concurrency, status tracking, retry, stop,
and monitoring. You just write the business logic.

---

### What You Get — Out of the Box

| Capability | Details |
|---|---|
| **REST API** | Launch, stop, retry, and query jobs — zero controller code needed |
| **Annotation-driven jobs** | `@BatchJob` + `JobHandler` — that's the entire programming model |
| **Concurrency control** | `maxConcurrency` per job — extras are automatically queued |
| **Virtual threads** | Java 21 `Executors.newVirtualThreadPerTaskExecutor()` — thousands of concurrent I/O-bound jobs without thread starvation |
| **Pluggable backends** | H2 (dev) → PostgreSQL (production) → Kafka (high-throughput) — switch with one YAML property, no code changes |
| **Fault tolerance** | Heartbeat-based node recovery — if a node dies, orphaned jobs are re-queued on healthy nodes automatically |
| **Kubernetes scaling** | Built-in Micrometer metrics + HPA / KEDA examples — auto-scale pods based on queue depth or Kafka lag |
| **Real-time dashboard** | React/MUI aggregator dashboard — monitor all nodes from a single pane of glass |
| **Retry & stop** | One-click retry of failed jobs, graceful stop of running jobs — via REST or dashboard |
| **Parameter validation** | `requiredParams` in the annotation — bad requests get an RFC 7807 error before the job ever starts |
| **Cloud-ready** | Deployment guides for AWS (EKS/ECS), Azure (AKS), GCP (GKE/Cloud Run), and Red Hat OpenShift |

---

### The Math: Build vs. Fluffy

| What You Need | Typical DIY Effort | With Fluffy |
|---|---|---|
| REST API for job CRUD | 2–3 weeks | **Already included** |
| Concurrency limiting + queuing | 1–2 weeks | **One annotation attribute** |
| Multi-node coordination | 3–4 weeks | **One YAML property** |
| Fault tolerance + node recovery | 2–3 weeks | **One YAML flag** |
| Kubernetes autoscaling integration | 1–2 weeks | **Built-in metrics + manifests** |
| Monitoring dashboard | 2–4 weeks | **Included (React/MUI)** |
| **Total** | **11–18 weeks** | **An afternoon** |

Every week you spend building batch infrastructure is a week you're not
building the product your users are paying for.

---

### Who Should Use Fluffy?

**Any Java / Spring Boot team that processes work in batches.**

- 🏥 **Healthcare** — EHR sync, claim processing, lab reports, HIPAA exports
- 🏦 **Financial services** — settlement, reconciliation, regulatory reports,
  fraud screening
- 📱 **Social media** — content moderation, notification fan-out, analytics
  roll-up, data exports
- 🏢 **Enterprise** — data migration, ETL, scheduled clean-up, file processing
- 🚀 **Startups** — production-grade batch processing without hiring an
  infrastructure team

---

### What Makes Fluffy Different?

1. **Spring Boot native** — auto-configuration, not a separate service to
   deploy. It lives inside your app.
2. **Zero boilerplate** — no XML, no job repository configuration, no step
   definitions. Annotate and go.
3. **Modern Java** — records, virtual threads, `Instant`, sealed classes,
   pattern matching. Built for Java 21+.
4. **Pluggable, not opinionated** — start with H2, graduate to PostgreSQL or
   Kafka when you're ready. Same code, different config.
5. **Kubernetes-first** — metrics, scaling APIs, recovery, and multi-node
   coordination are built in, not bolted on.
6. **Battle-tested** — 95% code coverage enforced by JaCoCo in CI. The
   framework is tested so your jobs don't have to test the framework.

---

### Get Started in Five Minutes

1. **Add the dependency** to your `pom.xml`.
2. **Create a job class** — annotate with `@BatchJob`, implement `JobHandler`.
3. **Start the app** — `mvn spring-boot:run`.
4. **Launch a job** — `curl -X POST .../api/jobs/{name}/start`.
5. **Open the dashboard** — `http://localhost:8080/fluffy-dashboard/index.html`.

Full walkthrough: [Developer Guide →](developer-guide.md)

---

### Ready for Production

```yaml
fluffy:
  batch:
    backend:
      type: DATABASE        # shared PostgreSQL queue
    recovery:
      enabled: true          # heartbeat + auto-recovery
    scaling:
      max-queue-depth: 100   # autoscaling signal
    dashboard:
      enabled: true
      title: "My Batch Dashboard"
```

Deploy to Kubernetes with the included manifests. Scale with HPA or KEDA.
Monitor with the aggregator dashboard. Sleep well knowing the recovery manager
re-queues orphaned jobs automatically.

Cloud deployment guides:
[AWS](setup-aws.md) · [Azure](setup-azure.md) · [GCP](setup-gcp.md) · [OpenShift](setup-rhos.md)

---

### The Bottom Line

**Fluffy turns months of batch infrastructure work into a single dependency.**

Stop building frameworks. Start shipping features.

```xml
<dependency>
  <groupId>com.fluffy</groupId>
  <artifactId>fluffy-batch-starter</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```

🐾 **Fluffy. Because your next batch job shouldn't take three sprints.**

[GitHub](https://github.com/iamankushpandit/fluffy) ·
[Developer Guide](developer-guide.md) ·
[Apache 2.0 License](../LICENSE) ·
[Contributing](../CONTRIBUTING.md)
