# Fluffy Batch Example

A demo application showcasing [Fluffy Batch Starter](../README.md) — a lightweight Spring Boot starter for batch job processing with virtual threads, concurrency control, and a built-in dashboard.

## What's Included

| Job | Type | Description |
|---|---|---|
| `data-sync` | Synchronous | Simulates a data synchronization task between two systems |
| `report-generation` | Async | Generates a report; requires a `reportType` parameter; supports graceful stop |
| `long-running` | Async | Demonstrates stop/timeout support with 60 iterations of 1-second work |

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
java -jar target/fluffy-batch-example-1.0.0-SNAPSHOT.jar
```

The app starts on **http://localhost:8080** with an H2 in-memory database.

- **Dashboard**: http://localhost:8080/fluffy-dashboard/index.html
- **H2 Console**: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:fluffydb`)
- **API Base**: http://localhost:8080/api/jobs

## Running with PostgreSQL

The example ships with a `postgres` Spring profile that switches the data source to PostgreSQL.

```bash
java -jar target/fluffy-batch-example-1.0.0-SNAPSHOT.jar \
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

# Check job status
curl http://localhost:8080/api/jobs/1/status

# Stop a running job
curl -X POST http://localhost:8080/api/jobs/1/stop

# Retry a completed/failed job
curl -X POST http://localhost:8080/api/jobs/1/retry

# List all executions
curl http://localhost:8080/api/jobs/executions

# List registered jobs
curl http://localhost:8080/api/jobs/registered
```

## Docker

```bash
# Build the image
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
1. Verify / install Minikube, kubectl, and Docker
2. Build the parent Maven project
3. Build the example Docker image inside Minikube's Docker daemon
4. Deploy PostgreSQL and the example app to the `fluffy` namespace
5. Print the URL to access the application

### Manual Kubernetes Deployment

```bash
# Create namespace
kubectl create namespace fluffy

# Deploy PostgreSQL
kubectl apply -f k8s/postgres.yaml -n fluffy

# Deploy the example app
kubectl apply -f k8s/app.yaml -n fluffy

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
fluffy-batch-example/
├── src/
│   ├── main/
│   │   ├── java/com/fluffy/example/
│   │   │   ├── ExampleApplication.java       # Spring Boot entry point
│   │   │   └── jobs/
│   │   │       ├── DataSyncJob.java           # Sync job example
│   │   │       ├── ReportGenerationJob.java   # Async job with params
│   │   │       └── LongRunningJob.java        # Stop/timeout demo
│   │   └── resources/
│   │       ├── application.yml                # Default config (H2)
│   │       └── application-postgres.yml       # PostgreSQL profile
│   └── test/
│       ├── java/com/fluffy/example/
│       │   ├── ExampleApplicationTest.java
│       │   └── jobs/
│       │       ├── DataSyncJobTest.java
│       │       ├── ReportGenerationJobTest.java
│       │       └── LongRunningJobTest.java
│       └── resources/
│           └── application.yml                # Test config (H2)
├── Dockerfile
├── pom.xml
└── README.md
```

## License

This project is licensed under the [Apache License 2.0](../LICENSE).
