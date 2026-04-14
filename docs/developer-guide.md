# Developer Guide — A Day in the Life of a Fluffy Developer

This guide walks you through a typical developer workflow: cloning the project,
writing a new batch job, running and testing it locally, and verifying it on
Kubernetes.

---

## 1. Set Up Your Environment

### Required tools

| Tool | Version | Install |
|---|---|---|
| Java (JDK) | **21+** | [Adoptium](https://adoptium.net/) or `brew install --cask temurin@21` |
| Maven | **3.8+** | `brew install maven` |
| Docker Desktop | latest | [docker.com](https://www.docker.com/products/docker-desktop/) |
| Minikube | latest | `brew install minikube` |
| kubectl | latest | `brew install kubernetes-cli` |

### Clone and build

```bash
git clone https://github.com/iamankushpandit/fluffy.git
cd fluffy
mvn clean package -DskipTests
```

This builds both the `fluffy-batch-starter` library and the `fluffy-batch-example`
demo application.

---

## 2. Run the Example App Locally

```bash
cd fluffy-batch-starter/fluffy-batch-example
mvn spring-boot:run
```

The app starts on port **8080** with the H2 in-memory backend (default profile).

Open the dashboard:

```
http://localhost:8080/fluffy-dashboard/index.html
```

> **H2 Console** is also available at `http://localhost:8080/h2-console`
> (JDBC URL: `jdbc:h2:mem:fluffydb`, user: `sa`, password: empty).

---

## 3. Write Your First Job

### Step 1 — Create a job class

Add a new class in `fluffy-batch-example/src/main/java/com/fluffy/example/jobs/`:

```java
@BatchJob(
    name        = "hello-world",
    description = "Prints a greeting and simulates work",
    async       = true,
    maxConcurrency = 2,
    timeoutSeconds = 60
)
public class HelloWorldJob implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(HelloWorldJob.class);

    @Override
    public void execute(JobContext context) throws Exception {
        String recipient = context.getParam("recipient");
        if (recipient == null) recipient = "World";

        log.info("Hello, {}! (executionId={})", recipient, context.getExecutionId());

        for (int i = 1; i <= 5; i++) {
            context.checkInterrupted();   // honour stop requests
            log.info("Working... step {}/5", i);
            Thread.sleep(1000);
        }

        log.info("HelloWorldJob complete");
    }
}
```

Spring component scanning picks it up automatically — no registration step
needed.

### Step 2 — Start the job via REST

```bash
curl -X POST http://localhost:8080/api/jobs/hello-world/start \
     -H "Content-Type: application/json" \
     -d '{"parameters": {"recipient": "Developer"}, "requestedBy": "me"}'
```

### Step 3 — Check the status

```bash
# List all executions
curl http://localhost:8080/api/jobs/executions

# Status of a specific execution (replace 1 with the returned ID)
curl http://localhost:8080/api/jobs/executions/1
```

---

## 4. Add Required Parameters

Use `requiredParams` in the annotation and `context.requireParam()` in the
handler:

```java
@BatchJob(
    name           = "data-export",
    requiredParams = {"format", "destination"}
)
public class DataExportJob implements JobHandler {

    @Override
    public void execute(JobContext context) throws Exception {
        String format      = context.requireParam("format");      // throws if missing
        String destination = context.requireParam("destination");
        // ...
    }
}
```

Calling the job without `format` or `destination` returns HTTP 400 with a
clear error message.

---

## 5. Run Tests

```bash
# All tests (unit + integration) with coverage
mvn clean test

# Starter library only
cd fluffy-batch-starter
mvn clean test

# Example app only
cd fluffy-batch-starter/fluffy-batch-example
mvn clean test
```

Coverage thresholds are enforced by JaCoCo (95% for the starter, 90% for the
example). A failing threshold blocks the build.

---

## 6. Try Different Backends Locally

### PostgreSQL backend

```bash
# Start Postgres
docker run -d --name fluffy-pg \
  -e POSTGRES_DB=fluffydb -e POSTGRES_USER=fluffy -e POSTGRES_PASSWORD=fluffy \
  -p 5432:5432 postgres:16

# Run with postgres profile
cd fluffy-batch-starter/fluffy-batch-example
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

### Kafka backend

```bash
# Start Kafka (KRaft, no ZooKeeper)
docker run -d --name fluffy-kafka \
  -e KAFKA_KRAFT_CLUSTER_ID=fluffy-local \
  -p 9092:9092 bitnami/kafka:latest

# Run with kafka profile
mvn spring-boot:run -Dspring-boot.run.profiles=kafka
```

---

## 7. Deploy to Local Kubernetes

Run the provided one-shot script from the repository root:

**macOS / Linux:**

```bash
chmod +x setup-and-deploy.sh
./setup-and-deploy.sh
```

**Windows (PowerShell):**

```powershell
.\setup-and-deploy.ps1
```

The script:
1. Verifies and installs prerequisites.
2. Tears down any previous deployment.
3. Starts Minikube.
4. Builds the Maven project and Docker image.
5. Deploys PostgreSQL, Kafka, and all four application profiles to the
   `fluffy-example-1` namespace.
6. Deploys the Aggregator dashboard.
7. Prints access URLs for every service.
8. Launches the Minikube Kubernetes dashboard and prints its URL.

---

## 8. Explore the Dashboards

| Dashboard | URL (local K8s) | Description |
|---|---|---|
| Per-node (postgres) | `<minikube-ip>:30080/fluffy-dashboard` | Job dashboard for the postgres instance |
| Per-node (H2) | `<minikube-ip>:30081/fluffy-dashboard` | Job dashboard for the H2 instance |
| Per-node (DB) | `<minikube-ip>:30082/fluffy-dashboard` | Job dashboard for the database instance |
| Per-node (Kafka) | `<minikube-ip>:30083/fluffy-dashboard` | Job dashboard for the Kafka instance |
| Aggregator | `<minikube-ip>:30084/fluffy-aggregator` | Multi-node React/MUI view across all nodes |
| Minikube K8s | printed by script | Kubernetes control-plane dashboard |

---

## 9. Common Developer Workflows

### Watch logs in real time

```bash
# Watch a specific app instance
kubectl logs -f -l app=fluffy-batch-h2 -n fluffy-example-1

# Watch the aggregator
kubectl logs -f -l app=fluffy-aggregator -n fluffy-example-1
```

### Tear down and redeploy cleanly

```bash
kubectl delete namespace fluffy-example-1
./setup-and-deploy.sh
```

### Iterate on job code without full Kubernetes rebuild

1. Edit the job class.
2. `mvn clean package -DskipTests` from the repo root.
3. Re-run the example app with `mvn spring-boot:run` for fast local iteration.
4. Run `./setup-and-deploy.sh` only when you need to verify on Kubernetes.

### Stop a running job via REST

```bash
curl -X POST http://localhost:8080/api/jobs/executions/1/stop
```

### Retry a failed job

```bash
curl -X POST http://localhost:8080/api/jobs/executions/1/retry
```

---

## 10. Configuration Quick-Reference

All Fluffy properties live under the `fluffy.batch` namespace in
`application.yml`:

```yaml
fluffy:
  batch:
    backend:
      type: H2           # H2 | DATABASE | KAFKA
    dashboard:
      enabled: true
      title: "My Dashboard"
    aggregator:
      enabled: false
      nodes:
        - http://node1:8080
        - http://node2:8080
    recovery:
      enabled: false
    metrics:
      enabled: true
    scaling:
      max-queue-depth: 100
```

See individual doc pages for deep-dives:
[Backends](backends.md) · [Dashboard](dashboard.md) · [Aggregator](aggregator.md) ·
[Scaling](scaling.md) · [Recovery](recovery.md) · [Metrics](metrics.md)
