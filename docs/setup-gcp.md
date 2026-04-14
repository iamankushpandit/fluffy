# Deploying Fluffy on Google Cloud Platform

This guide covers common patterns for running Fluffy Batch Starter on Google Cloud Platform (GCP).

---

## Architecture Overview

```
                         ┌──────────────────────┐
                         │  Cloud Load Balancer  │
                         └──────────┬───────────┘
                                    │
            ┌───────────────────────┼───────────────────────┐
            ▼                       ▼                       ▼
   ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
   │   Fluffy Node 1 │   │   Fluffy Node 2 │   │   Fluffy Node N │
   │ (GKE/Cloud Run) │   │ (GKE/Cloud Run) │   │ (GKE/Cloud Run) │
   └────────┬────────┘   └────────┬────────┘   └────────┬────────┘
            │                     │                     │
            └─────────────────────┼─────────────────────┘
                                  │
                    ┌─────────────┴─────────────┐
                    ▼                           ▼
          ┌──────────────────┐       ┌──────────────────┐
          │    Cloud SQL     │       │  Managed Kafka   │
          │  (PostgreSQL)    │       │  (or Pub/Sub)    │
          └──────────────────┘       └──────────────────┘
```

---

## Option 1 — Google Kubernetes Engine (GKE)

GKE provides a managed Kubernetes environment. Fluffy's existing Kubernetes manifests work directly.

### Prerequisites

- Google Cloud CLI (`gcloud`) installed and authenticated
- A GCP project with billing enabled

### 1. Create the GKE Cluster

```bash
# Set project
gcloud config set project <PROJECT_ID>

# Create cluster
gcloud container clusters create fluffy-cluster \
  --zone us-central1-a \
  --machine-type e2-medium \
  --num-nodes 3 \
  --enable-autoscaling \
  --min-nodes 1 \
  --max-nodes 6
```

### 2. Build and Push to Artifact Registry

```bash
# Create repository
gcloud artifacts repositories create fluffy-repo \
  --repository-format=docker \
  --location=us-central1

# Configure Docker auth
gcloud auth configure-docker us-central1-docker.pkg.dev

# Build and push
cd fluffy-batch-starter/fluffy-batch-example
docker build -t us-central1-docker.pkg.dev/<PROJECT_ID>/fluffy-repo/fluffy-batch-example:latest .
docker push us-central1-docker.pkg.dev/<PROJECT_ID>/fluffy-repo/fluffy-batch-example:latest
```

### 3. Provision Cloud SQL for PostgreSQL

```bash
# Create instance
gcloud sql instances create fluffy-db \
  --database-version=POSTGRES_16 \
  --tier=db-f1-micro \
  --region=us-central1 \
  --root-password=<SECURE_PASSWORD>

# Create database
gcloud sql databases create fluffydb --instance=fluffy-db

# Create user
gcloud sql users create fluffy \
  --instance=fluffy-db \
  --password=<SECURE_PASSWORD>
```

For GKE, use the [Cloud SQL Auth Proxy](https://cloud.google.com/sql/docs/postgres/connect-kubernetes-engine) as a sidecar:

```yaml
containers:
  - name: fluffy-batch-db
    image: us-central1-docker.pkg.dev/<PROJECT_ID>/fluffy-repo/fluffy-batch-example:latest
    env:
      - name: SPRING_PROFILES_ACTIVE
        value: "database"
      - name: POSTGRES_HOST
        value: "127.0.0.1"
      - name: POSTGRES_PORT
        value: "5432"
  - name: cloud-sql-proxy
    image: gcr.io/cloud-sql-connectors/cloud-sql-proxy:2
    args:
      - "--structured-logs"
      - "<PROJECT_ID>:us-central1:fluffy-db"
    securityContext:
      runAsNonRoot: true
```

### 4. Store Credentials

```bash
kubectl create namespace fluffy

kubectl create secret generic postgres-secret \
  -n fluffy \
  --from-literal=POSTGRES_DB=fluffydb \
  --from-literal=POSTGRES_USER=fluffy \
  --from-literal=POSTGRES_PASSWORD=<SECURE_PASSWORD>
```

### 5. Deploy

```bash
kubectl apply -f k8s/app-db.yaml -n fluffy
kubectl rollout status deployment/fluffy-batch-db -n fluffy --timeout=180s
```

### 6. Autoscaling

```bash
kubectl apply -f k8s/hpa.yaml -n fluffy
```

For queue-depth-based scaling, install [KEDA](https://keda.sh/) on GKE. See the [Scaling Configuration](scaling.md) guide for `ScaledObject` examples.

---

## Option 2 — Cloud Run

Cloud Run provides a fully managed, serverless container platform.

### 1. Deploy to Cloud Run

```bash
gcloud run deploy fluffy-batch \
  --image us-central1-docker.pkg.dev/<PROJECT_ID>/fluffy-repo/fluffy-batch-example:latest \
  --platform managed \
  --region us-central1 \
  --port 8080 \
  --memory 1Gi \
  --cpu 1 \
  --min-instances 1 \
  --max-instances 10 \
  --set-env-vars \
    SPRING_PROFILES_ACTIVE=database,\
    POSTGRES_HOST=/cloudsql/<PROJECT_ID>:us-central1:fluffy-db,\
    POSTGRES_PORT=5432 \
  --set-secrets \
    POSTGRES_USER=fluffy-db-user:latest,\
    POSTGRES_PASSWORD=fluffy-db-pass:latest \
  --add-cloudsql-instances <PROJECT_ID>:us-central1:fluffy-db
```

### 2. Scaling Configuration

Cloud Run scales automatically based on incoming requests. Adjust concurrency settings:

```bash
gcloud run services update fluffy-batch \
  --concurrency 20 \
  --min-instances 1 \
  --max-instances 10
```

> **Note:** Cloud Run's request-based scaling model works best with the H2 backend for stateless, single-request jobs. For multi-node coordination with Database or Kafka backends, GKE is recommended.

---

## Option 3 — Kafka Backend with Managed Service for Apache Kafka

For high-throughput distributed deployments, use Google Cloud Managed Service for Apache Kafka.

### 1. Create a Kafka Cluster

```bash
gcloud managed-kafka clusters create fluffy-kafka \
  --location=us-central1 \
  --cpu=3 \
  --memory=3GB \
  --subnets=projects/<PROJECT_ID>/regions/us-central1/subnetworks/default
```

### 2. Create the Topic

```bash
gcloud managed-kafka topics create fluffy-jobs \
  --location=us-central1 \
  --cluster=fluffy-kafka \
  --partitions=6 \
  --replication-factor=3
```

### 3. Application Configuration

```yaml
fluffy:
  batch:
    backend:
      type: KAFKA
      kafka:
        bootstrap-servers: bootstrap.fluffy-kafka.us-central1.managedkafka.<PROJECT_ID>.cloud.goog:9092
        topic: fluffy-jobs
        group-id: fluffy-batch

spring:
  datasource:
    url: jdbc:postgresql://127.0.0.1:5432/fluffydb
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
```

---

## Monitoring

### Cloud Monitoring

Enable GKE metrics collection in Cloud Monitoring for out-of-the-box container and pod metrics. For Fluffy-specific Prometheus metrics, use [Google Cloud Managed Service for Prometheus](https://cloud.google.com/stackdriver/docs/managed-prometheus):

```bash
# Enable managed Prometheus on GKE
gcloud container clusters update fluffy-cluster \
  --zone us-central1-a \
  --enable-managed-prometheus
```

Create a `PodMonitoring` resource to scrape Fluffy metrics:

```yaml
apiVersion: monitoring.googleapis.com/v1
kind: PodMonitoring
metadata:
  name: fluffy-metrics
  namespace: fluffy
spec:
  selector:
    matchLabels:
      app: fluffy-batch-db
  endpoints:
    - port: 8080
      path: /actuator/prometheus
      interval: 15s
```

### Key Metrics

| Metric | Alerting Policy Suggestion |
|---|---|
| `fluffy_batch_queue_depth` | > 50 for 5 minutes |
| `fluffy_batch_jobs_active` | Near max concurrency for 10 minutes |
| `fluffy_batch_jobs_failed_total` | Rate > 0.1/s for 5 minutes |

### Cloud Trace

Add the [OpenTelemetry Java agent](https://cloud.google.com/trace/docs/setup/java) for distributed tracing:

```dockerfile
ENV JAVA_TOOL_OPTIONS="-javaagent:/app/opentelemetry-javaagent.jar"
ENV OTEL_EXPORTER_OTLP_ENDPOINT=https://monitoring.googleapis.com
```

---

## Security Best Practices

- Store credentials in **Secret Manager** and reference them in GKE pods via the [Secret Manager add-on](https://cloud.google.com/secret-manager/docs/using-other-products#google-kubernetes-engine) or in Cloud Run with `--set-secrets`.
- Use the **Cloud SQL Auth Proxy** instead of exposing the database with a public IP.
- Enable **Workload Identity** for GKE pods to access GCP services without service account keys.
- Use **VPC-native clusters** with private nodes for network isolation.
- Enable **Cloud SQL encryption** (enabled by default) and enforce SSL connections.

---

## Cost Optimization

| Component | Cost Tip |
|---|---|
| GKE nodes | Use [Spot VMs](https://cloud.google.com/kubernetes-engine/docs/concepts/spot-vms) for batch worker nodes |
| Cloud Run | Pay per request — ideal for bursty batch workloads |
| Cloud SQL | Use `db-f1-micro` for dev; `db-custom-2-7680` for production |
| Managed Kafka | Consider Pub/Sub for simpler use cases with lower cost |

---

## Further Reading

- [Backend Configuration](backends.md) — Choosing H2, Database, or Kafka mode
- [Scaling Configuration](scaling.md) — HPA and KEDA autoscaling examples
- [Fault Tolerance & Recovery](recovery.md) — Node heartbeat and job recovery
- [Metrics Reference](metrics.md) — Prometheus metrics and Grafana queries
