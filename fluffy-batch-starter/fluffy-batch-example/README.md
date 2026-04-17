# Fluffy Batch Example

A demo application showcasing [Fluffy Batch Starter](../../README.md) — a lightweight Spring Boot starter for batch job processing with virtual threads, concurrency control, and a built-in dashboard.

## What's Included

| Job | Type | Execution Mode | Description |
|---|---|---|---|
| `data-sync` | Async | LOCAL | Simulates a data synchronization task between two systems |
| `report-generation` | Async | LOCAL | Generates a report; requires a `reportType` parameter; supports graceful stop |
| `long-running` | Async | LOCAL | Demonstrates stop/timeout support with 60 iterations of 1-second work |
| `cloud-native-etl` | Async | CLOUD_NATIVE | ETL pipeline dispatched to an external cloud-native orchestrator (K8s, Airflow, AWS Batch) |

## Prerequisites

| Technology | Version |
|---|---|
| Java (JDK) | **21** or later |
| Maven | **3.8** or later |
| Docker | **20+** (for containerized deployment) |
| Minikube / kind | Latest (for local Kubernetes deployment) |

## Quick Start (H2 — in-memory)

```bash
# Build from the repository root
mvn clean package -DskipTests

# Run the example app
java -jar fluffy-batch-starter/fluffy-batch-example/target/fluffy-batch-example-1.0.0-SNAPSHOT.jar
```

The app starts on **http://localhost:8080** with an H2 in-memory database.

- **Dashboard**: http://localhost:8080/fluffy-dashboard/index.html  *(enabled by default in the example)*
- **H2 Console**: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:fluffydb`)
- **API Base**: http://localhost:8080/api/jobs

> **Note:** The dashboard is disabled by default in the starter library.
> The example enables it via `fluffy.batch.dashboard.enabled=true` in `application.yml`.

## Running with PostgreSQL

The example ships with a `postgres` Spring profile that switches the data source to PostgreSQL.

```bash
java -jar fluffy-batch-starter/fluffy-batch-example/target/fluffy-batch-example-1.0.0-SNAPSHOT.jar \
     --spring.profiles.active=postgres
```

By default the profile connects to `localhost:5432/fluffydb` with user `fluffy` / password `fluffy`.
Override with environment variables:

| Variable | Default |
|---|---|
| `POSTGRES_HOST` | `localhost` |
| `POSTGRES_PORT` | `5432` |
| `POSTGRES_DB` | `fluffydb` |
| `POSTGRES_USER` | `fluffy` |
| `POSTGRES_PASSWORD` | `fluffy` |

## REST API Cheat-Sheet

```bash
# Start a job
curl -X POST http://localhost:8080/api/jobs/data-sync/start \
     -H "Content-Type: application/json" \
     -d '{"parameters":{"source":"sysA","destination":"sysB"}}'

# Start a job with required parameter
curl -X POST http://localhost:8080/api/jobs/report-generation/start \
     -H "Content-Type: application/json" \
     -d '{"parameters":{"reportType":"monthly","dateRange":"2024-Q1"}}'

# Start the cloud-native ETL job (dispatches to external orchestrator when cloud-native profile is active)
curl -X POST http://localhost:8080/api/jobs/cloud-native-etl/start \
     -H "Content-Type: application/json" \
     -d '{"parameters":{"pipeline":"daily-ingest","stage":"extract"}}'

# Check job status
curl http://localhost:8080/api/jobs/1/status

# Stop a running job
curl -X POST http://localhost:8080/api/jobs/1/stop

# Retry a completed/failed job
curl -X POST http://localhost:8080/api/jobs/1/retry

# List all executions
curl http://localhost:8080/api/jobs/executions

# List registered jobs (shows executionMode for each job)
curl http://localhost:8080/api/jobs/registered
```

## Cloud-Native Execution

Jobs annotated with `executionMode = ExecutionMode.CLOUD_NATIVE` can be dispatched to an
external orchestrator (Kubernetes CronJob, Apache Airflow, AWS Batch, Google Cloud Composer,
Azure Logic Apps, HashiCorp Nomad, etc.) instead of running in-process.

### How It Works

1. **Dispatch**: When a cloud-native job is triggered, Fluffy sends an HTTP POST to the
   configured endpoint with the job name, execution ID, parameters, and a callback URL.
2. **Execute**: The external orchestrator runs the containerised job. The job can be any
   stateless container — it doesn't need to use Fluffy's runtime.
3. **Callback**: When the job finishes, the orchestrator POSTs a status update
   (`COMPLETED`, `FAILED`, or `STOPPED`) back to Fluffy's callback endpoint.

### Configuration

Activate the `cloud-native` Spring profile:

```bash
java -jar fluffy-batch-example.jar --spring.profiles.active=cloud-native
```

Or set environment variables:

| Variable | Default | Description |
|---|---|---|
| `CLOUD_NATIVE_ENDPOINT` | `http://localhost:9090/api/dispatch` | URL to POST job dispatch requests to |
| `CLOUD_NATIVE_CALLBACK_URL` | `http://localhost:8080/api/jobs/callback` | URL the orchestrator POSTs status updates back to |

### Callback Format

The external orchestrator should POST to the callback URL with:

```json
{
  "executionId": "123",
  "status": "COMPLETED",
  "errorMessage": "optional error details"
}
```

Valid status values: `COMPLETED`, `FAILED`, `STOPPED`.

### Defining Cloud-Native Jobs

```java
@BatchJob(
    name = "my-etl-pipeline",
    description = "ETL pipeline for data warehouse",
    executionMode = ExecutionMode.CLOUD_NATIVE,
    requiredParams = {"pipeline"},
    maxConcurrency = 2
)
public class MyEtlJob implements JobHandler {
    @Override
    public void execute(JobContext context) throws Exception {
        // This handler runs when executed locally (tests, dev mode)
        // or when the orchestrator runs the containerised app.
        String pipeline = context.requireParam("pipeline");
        // ... do ETL work ...
    }
}
```

### Programmatic Registration

```java
jobRegistry.register(JobDefinition.builder("my-cloud-job")
    .handler(ctx -> { /* ... */ })
    .executionMode(ExecutionMode.CLOUD_NATIVE)
    .requiredParams("input")
    .build());
```

## Docker

```bash
# Build the image (from this directory)
docker build -t fluffy-batch-example:latest .

# Run with H2
docker run -p 8080:8080 fluffy-batch-example:latest

# Run with external PostgreSQL
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=postgres \
  -e POSTGRES_HOST=host.docker.internal \
  fluffy-batch-example:latest
```

## Local Kubernetes Deployment

A full setup script is provided for Windows developers. It installs prerequisites, builds the project, creates a Docker image, and deploys to a local Minikube cluster with a live PostgreSQL database.

```powershell
# From the repository root
.\setup-and-deploy.ps1
```

The script will:
1. Verify / install Minikube, kubectl, and Docker (skips if already installed)
2. Tear down any existing Fluffy deployment
3. Build the parent Maven project
4. Build the example Docker image inside Minikube's Docker daemon
5. Deploy PostgreSQL and the example app to the `fluffy` namespace
6. Verify each step before proceeding
7. Print the dashboard URL and other access details

### Manual Kubernetes Deployment

```bash
# Create namespace
kubectl create namespace fluffy

# Deploy PostgreSQL
kubectl apply -f fluffy-batch-starter/fluffy-batch-example/k8s/postgres.yaml -n fluffy

# Deploy the example app
kubectl apply -f fluffy-batch-starter/fluffy-batch-example/k8s/app.yaml -n fluffy

# Access the app
minikube service fluffy-batch-example -n fluffy
```

## Running Tests

```bash
# From the repository root
mvn clean test
```

Tests use an H2 in-memory database and require no external services.

## Project Structure

```
fluffy-batch-starter/
└── fluffy-batch-example/
    ├── src/
    │   ├── main/
    │   │   ├── java/com/fluffy/example/
    │   │   │   ├── ExampleApplication.java       # Spring Boot entry point
    │   │   │   └── jobs/
    │   │   │       ├── DataSyncJob.java           # Sync job example
    │   │   │       ├── ReportGenerationJob.java   # Async job with params
    │   │   │       ├── LongRunningJob.java        # Stop/timeout demo
    │   │   │       └── CloudNativeEtlJob.java     # Cloud-native execution demo
    │   │   └── resources/
    │   │       ├── application.yml                # Default config (H2, dashboard enabled)
    │   │       ├── application-postgres.yml       # PostgreSQL profile
    │   │       └── application-cloud-native.yml   # Cloud-native execution profile
    │   └── test/
    │       ├── java/com/fluffy/example/
    │       │   ├── ExampleApplicationTest.java
    │       │   └── jobs/
    │       │       ├── DataSyncJobTest.java
    │       │       ├── ReportGenerationJobTest.java
    │       │       ├── LongRunningJobTest.java
    │       │       └── CloudNativeEtlJobTest.java
    │       └── resources/
    │           └── application.yml                # Test config (H2)
    ├── k8s/
    │   ├── app.yaml                               # K8s Deployment + Service
    │   └── postgres.yaml                          # PostgreSQL Deployment + Service
    ├── Dockerfile
    ├── pom.xml
    └── README.md
```

## License

This project is licensed under the [Apache License 2.0](../../LICENSE).
