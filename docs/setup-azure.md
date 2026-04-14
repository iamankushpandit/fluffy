# Deploying Fluffy on Microsoft Azure

This guide covers common patterns for running Fluffy Batch Starter on Microsoft Azure.

---

## Architecture Overview

```
                         ┌──────────────────────┐
                         │  Azure Load Balancer  │
                         │   / Application GW    │
                         └──────────┬───────────┘
                                    │
            ┌───────────────────────┼───────────────────────┐
            ▼                       ▼                       ▼
   ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
   │   Fluffy Node 1 │   │   Fluffy Node 2 │   │   Fluffy Node N │
   │  (AKS / ACA)    │   │  (AKS / ACA)    │   │  (AKS / ACA)    │
   └────────┬────────┘   └────────┬────────┘   └────────┬────────┘
            │                     │                     │
            └─────────────────────┼─────────────────────┘
                                  │
                    ┌─────────────┴─────────────┐
                    ▼                           ▼
          ┌──────────────────┐       ┌──────────────────┐
          │ Azure Database   │       │ Azure Event Hubs │
          │ for PostgreSQL   │       │  (Kafka API)     │
          └──────────────────┘       └──────────────────┘
```

---

## Option 1 — Azure Kubernetes Service (AKS)

AKS is the recommended approach for production deployments. Fluffy's Kubernetes manifests work directly.

### Prerequisites

- Azure CLI (`az`) installed and authenticated
- An Azure Container Registry (ACR) for Docker images

### 1. Create the AKS Cluster

```bash
# Create resource group
az group create --name fluffy-rg --location eastus

# Create AKS cluster
az aks create \
  --resource-group fluffy-rg \
  --name fluffy-aks \
  --node-count 3 \
  --node-vm-size Standard_B2s \
  --enable-managed-identity \
  --generate-ssh-keys

# Get credentials
az aks get-credentials --resource-group fluffy-rg --name fluffy-aks
```

### 2. Build and Push to ACR

```bash
# Create ACR
az acr create --resource-group fluffy-rg --name fluffyacr --sku Basic

# Attach ACR to AKS
az aks update --resource-group fluffy-rg --name fluffy-aks --attach-acr fluffyacr

# Build and push
az acr build --registry fluffyacr \
  --image fluffy-batch-example:latest \
  --file fluffy-batch-starter/fluffy-batch-example/Dockerfile \
  fluffy-batch-starter/fluffy-batch-example
```

### 3. Provision Azure Database for PostgreSQL

```bash
az postgres flexible-server create \
  --resource-group fluffy-rg \
  --name fluffy-pg \
  --location eastus \
  --admin-user fluffy \
  --admin-password <SECURE_PASSWORD> \
  --sku-name Standard_B1ms \
  --tier Burstable \
  --storage-size 32 \
  --version 16

# Create the database
az postgres flexible-server db create \
  --resource-group fluffy-rg \
  --server-name fluffy-pg \
  --database-name fluffydb

# Allow AKS access (use private endpoints in production)
az postgres flexible-server firewall-rule create \
  --resource-group fluffy-rg \
  --name fluffy-pg \
  --rule-name allow-aks \
  --start-ip-address <AKS_OUTBOUND_IP> \
  --end-ip-address <AKS_OUTBOUND_IP>
```

Store credentials as a Kubernetes Secret:

```bash
kubectl create namespace fluffy

kubectl create secret generic postgres-secret \
  -n fluffy \
  --from-literal=POSTGRES_DB=fluffydb \
  --from-literal=POSTGRES_USER=fluffy \
  --from-literal=POSTGRES_PASSWORD=<SECURE_PASSWORD>
```

### 4. Update Kubernetes Manifests

Modify `k8s/app-db.yaml` to use the ACR image and Azure PostgreSQL endpoint:

```yaml
containers:
  - name: fluffy-batch-db
    image: fluffyacr.azurecr.io/fluffy-batch-example:latest
    imagePullPolicy: Always
    env:
      - name: SPRING_PROFILES_ACTIVE
        value: "database"
      - name: POSTGRES_HOST
        value: "fluffy-pg.postgres.database.azure.com"
      - name: POSTGRES_PORT
        value: "5432"
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

For advanced queue-depth-based scaling, install [KEDA](https://keda.sh/) — it is a CNCF project that was originally developed at Microsoft and integrates natively with AKS:

```bash
az aks update --resource-group fluffy-rg --name fluffy-aks --enable-keda
```

See the [Scaling Configuration](scaling.md) guide for KEDA `ScaledObject` examples.

---

## Option 2 — Azure Container Apps

Azure Container Apps provides a serverless container platform with built-in scaling and no cluster management.

### 1. Create the Container Apps Environment

```bash
az containerapp env create \
  --resource-group fluffy-rg \
  --name fluffy-env \
  --location eastus
```

### 2. Deploy the Application

```bash
az containerapp create \
  --resource-group fluffy-rg \
  --name fluffy-batch \
  --environment fluffy-env \
  --image fluffyacr.azurecr.io/fluffy-batch-example:latest \
  --registry-server fluffyacr.azurecr.io \
  --target-port 8080 \
  --ingress external \
  --min-replicas 1 \
  --max-replicas 10 \
  --cpu 0.5 \
  --memory 1.0Gi \
  --env-vars \
    SPRING_PROFILES_ACTIVE=database \
    POSTGRES_HOST=fluffy-pg.postgres.database.azure.com \
    POSTGRES_PORT=5432 \
  --secrets \
    pg-user=<SECURE_USER> \
    pg-pass=<SECURE_PASSWORD> \
  --secret-volume-mount /secrets
```

### 3. Scaling Rules

Container Apps supports custom KEDA scaling rules natively:

```bash
az containerapp update \
  --resource-group fluffy-rg \
  --name fluffy-batch \
  --scale-rule-name queue-depth \
  --scale-rule-type http \
  --scale-rule-http-concurrency 20
```

For Prometheus-based scaling on `fluffy_batch_queue_depth`, configure a custom KEDA scaler in the Container App YAML.

---

## Option 3 — Kafka Backend with Azure Event Hubs

Azure Event Hubs provides a Kafka-compatible endpoint, so Fluffy's Kafka backend works without running Kafka brokers.

### 1. Create an Event Hubs Namespace

```bash
az eventhubs namespace create \
  --resource-group fluffy-rg \
  --name fluffy-events \
  --location eastus \
  --sku Standard \
  --enable-kafka true
```

### 2. Create the Topic (Event Hub)

```bash
az eventhubs eventhub create \
  --resource-group fluffy-rg \
  --namespace-name fluffy-events \
  --name fluffy-jobs \
  --partition-count 4 \
  --message-retention 1
```

### 3. Application Configuration

Use the Event Hubs Kafka endpoint with SASL authentication:

```yaml
fluffy:
  batch:
    backend:
      type: KAFKA
      kafka:
        bootstrap-servers: fluffy-events.servicebus.windows.net:9093
        topic: fluffy-jobs
        group-id: fluffy-batch

spring:
  kafka:
    properties:
      security.protocol: SASL_SSL
      sasl.mechanism: PLAIN
      sasl.jaas.config: >-
        org.apache.kafka.common.security.plain.PlainLoginModule required
        username="$ConnectionString"
        password="Endpoint=sb://fluffy-events.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=<KEY>";
  datasource:
    url: jdbc:postgresql://fluffy-pg.postgres.database.azure.com:5432/fluffydb
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
```

---

## Monitoring

### Azure Monitor

Use [Azure Monitor Container Insights](https://learn.microsoft.com/en-us/azure/azure-monitor/containers/container-insights-overview) for AKS clusters or built-in logging for Container Apps.

Enable Prometheus metrics collection in AKS:

```bash
az aks update \
  --resource-group fluffy-rg \
  --name fluffy-aks \
  --enable-azure-monitor-metrics
```

### Key Metrics

| Metric | Azure Monitor Alert Suggestion |
|---|---|
| `fluffy_batch_queue_depth` | > 50 for 5 minutes |
| `fluffy_batch_jobs_active` | Near max concurrency for 10 minutes |
| `fluffy_batch_jobs_failed_total` | Rate > 0.1/s for 5 minutes |

### Application Insights

Add the Application Insights Java agent to the Docker image for distributed tracing and request telemetry:

```dockerfile
FROM eclipse-temurin:21-jre-alpine
COPY applicationinsights-agent-3.x.x.jar /app/
ENV JAVA_TOOL_OPTIONS="-javaagent:/app/applicationinsights-agent-3.x.x.jar"
COPY target/fluffy-batch-example-*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

---

## Security Best Practices

- Use **Azure Key Vault** to store database passwords and Event Hubs connection strings. Mount them as Kubernetes secrets via the [Secrets Store CSI Driver](https://learn.microsoft.com/en-us/azure/aks/csi-secrets-store-driver).
- Place the PostgreSQL server in a **private VNet** and use a VNet-injected AKS cluster or Container Apps environment.
- Enable **Azure AD Workload Identity** for pod-level Azure access without managing credentials.
- Enable **TLS enforcement** on Azure Database for PostgreSQL.
- Use **managed identity** for ACR pull access instead of static credentials.

---

## Cost Optimization

| Component | Cost Tip |
|---|---|
| AKS nodes | Use [Spot node pools](https://learn.microsoft.com/en-us/azure/aks/spot-node-pool) for batch worker nodes |
| Container Apps | Serverless billing — pay only for active CPU/memory seconds |
| PostgreSQL | Use `Standard_B1ms` (Burstable) for dev; `Standard_D2ds_v4` for production |
| Event Hubs | Standard tier for dev; Premium for production high throughput |

---

## Further Reading

- [Backend Configuration](backends.md) — Choosing H2, Database, or Kafka mode
- [Scaling Configuration](scaling.md) — HPA and KEDA autoscaling examples
- [Fault Tolerance & Recovery](recovery.md) — Node heartbeat and job recovery
- [Metrics Reference](metrics.md) — Prometheus metrics and Grafana queries
