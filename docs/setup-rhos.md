# Deploying Fluffy on Red Hat OpenShift

This guide covers common patterns for running Fluffy Batch Starter on Red Hat OpenShift Container Platform (OCP).

---

## Architecture Overview

```
                         ┌──────────────────────┐
                         │   OpenShift Router    │
                         │   (HAProxy Ingress)   │
                         └──────────┬───────────┘
                                    │
            ┌───────────────────────┼───────────────────────┐
            ▼                       ▼                       ▼
   ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
   │   Fluffy Pod 1  │   │   Fluffy Pod 2  │   │   Fluffy Pod N  │
   │  (Deployment)   │   │  (Deployment)   │   │  (Deployment)   │
   └────────┬────────┘   └────────┬────────┘   └────────┬────────┘
            │                     │                     │
            └─────────────────────┼─────────────────────┘
                                  │
                    ┌─────────────┴─────────────┐
                    ▼                           ▼
          ┌──────────────────┐       ┌──────────────────┐
          │   PostgreSQL     │       │   AMQ Streams     │
          │ (Crunchy / OCP)  │       │   (Kafka)         │
          └──────────────────┘       └──────────────────┘
```

---

## Option 1 — OpenShift Deployment with PostgreSQL

### Prerequisites

- OpenShift CLI (`oc`) installed and logged in to the cluster
- Cluster admin or project admin permissions

### 1. Create a Project

```bash
oc new-project fluffy
```

### 2. Build the Image with OpenShift Builds

OpenShift can build images from source using S2I or Dockerfile:

```bash
# Using Dockerfile strategy
oc new-build --name=fluffy-batch-example \
  --strategy=docker \
  --binary \
  -n fluffy

# Start the build from local directory
cd fluffy-batch-starter/fluffy-batch-example
oc start-build fluffy-batch-example \
  --from-dir=. \
  --follow \
  -n fluffy
```

Alternatively, push a pre-built image to the OpenShift internal registry:

```bash
# Login to the internal registry
oc registry login

# Tag and push
docker tag fluffy-batch-example:latest \
  default-route-openshift-image-registry.apps.<CLUSTER_DOMAIN>/fluffy/fluffy-batch-example:latest
docker push \
  default-route-openshift-image-registry.apps.<CLUSTER_DOMAIN>/fluffy/fluffy-batch-example:latest
```

### 3. Deploy PostgreSQL

Use the OpenShift template for PostgreSQL:

```bash
oc new-app postgresql-persistent \
  --param=POSTGRESQL_USER=fluffy \
  --param=POSTGRESQL_PASSWORD=<SECURE_PASSWORD> \
  --param=POSTGRESQL_DATABASE=fluffydb \
  --param=VOLUME_CAPACITY=1Gi \
  -n fluffy
```

Or use the Crunchy PostgreSQL Operator for production-grade PostgreSQL. Install it from OperatorHub:

```bash
# After installing the operator from OperatorHub, create a cluster
cat <<EOF | oc apply -n fluffy -f -
apiVersion: postgres-operator.crunchydata.com/v1beta1
kind: PostgresCluster
metadata:
  name: fluffy-db
spec:
  postgresVersion: 16
  instances:
    - name: instance1
      replicas: 2
      dataVolumeClaimSpec:
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: 10Gi
  backups:
    pgbackrest:
      repos:
        - name: repo1
          volume:
            volumeClaimSpec:
              accessModes: ["ReadWriteOnce"]
              resources:
                requests:
                  storage: 5Gi
EOF
```

### 4. Create Secrets

```bash
oc create secret generic postgres-secret \
  -n fluffy \
  --from-literal=POSTGRES_DB=fluffydb \
  --from-literal=POSTGRES_USER=fluffy \
  --from-literal=POSTGRES_PASSWORD=<SECURE_PASSWORD>
```

### 5. Create the Deployment

OpenShift uses the same Kubernetes manifests. Update the image reference and apply:

```yaml
# k8s/app-db-ocp.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: fluffy-batch-db
  namespace: fluffy
  labels:
    app: fluffy-batch-db
    app.kubernetes.io/part-of: fluffy-batch
spec:
  replicas: 2
  selector:
    matchLabels:
      app: fluffy-batch-db
  template:
    metadata:
      labels:
        app: fluffy-batch-db
    spec:
      containers:
        - name: fluffy-batch-db
          image: image-registry.openshift-image-registry.svc:5000/fluffy/fluffy-batch-example:latest
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "database"
            - name: POSTGRES_HOST
              value: "postgresql"
            - name: POSTGRES_PORT
              value: "5432"
            - name: POSTGRES_DB
              valueFrom:
                secretKeyRef:
                  name: postgres-secret
                  key: POSTGRES_DB
            - name: POSTGRES_USER
              valueFrom:
                secretKeyRef:
                  name: postgres-secret
                  key: POSTGRES_USER
            - name: POSTGRES_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: postgres-secret
                  key: POSTGRES_PASSWORD
          readinessProbe:
            httpGet:
              path: /api/jobs/registered
              port: 8080
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /api/jobs/registered
              port: 8080
            periodSeconds: 15
          resources:
            requests:
              memory: "320Mi"
              cpu: "200m"
            limits:
              memory: "640Mi"
              cpu: "1000m"
```

```bash
oc apply -f k8s/app-db-ocp.yaml -n fluffy
```

### 6. Expose via Route

```bash
# Create a Service
oc expose deployment fluffy-batch-db --port=8080 -n fluffy

# Create a Route (OpenShift's equivalent of Ingress)
oc expose service fluffy-batch-db -n fluffy

# Get the URL
oc get route fluffy-batch-db -n fluffy -o jsonpath='{.spec.host}'
```

For TLS termination:

```bash
oc create route edge fluffy-batch-tls \
  --service=fluffy-batch-db \
  --port=8080 \
  -n fluffy
```

---

## Option 2 — Kafka Backend with AMQ Streams

Red Hat AMQ Streams is the supported Kafka distribution on OpenShift, based on the Strimzi operator.

### 1. Install AMQ Streams Operator

Install from OperatorHub in the OpenShift console, or via CLI:

```bash
cat <<EOF | oc apply -f -
apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: amq-streams
  namespace: openshift-operators
spec:
  channel: stable
  name: amq-streams
  source: redhat-operators
  sourceNamespace: openshift-marketplace
EOF
```

### 2. Create a Kafka Cluster

```yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: Kafka
metadata:
  name: fluffy-kafka
  namespace: fluffy
spec:
  kafka:
    version: 3.7.0
    replicas: 3
    listeners:
      - name: plain
        port: 9092
        type: internal
        tls: false
    config:
      offsets.topic.replication.factor: 3
      transaction.state.log.replication.factor: 3
      transaction.state.log.min.isr: 2
    storage:
      type: persistent-claim
      size: 10Gi
  zookeeper:
    replicas: 3
    storage:
      type: persistent-claim
      size: 5Gi
  entityOperator:
    topicOperator: {}
    userOperator: {}
```

```bash
oc apply -f kafka-cluster.yaml -n fluffy
```

### 3. Create the Topic

```yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: KafkaTopic
metadata:
  name: fluffy-jobs
  namespace: fluffy
  labels:
    strimzi.io/cluster: fluffy-kafka
spec:
  partitions: 6
  replicas: 3
  config:
    retention.ms: 604800000
```

```bash
oc apply -f kafka-topic.yaml -n fluffy
```

### 4. Application Configuration

```yaml
fluffy:
  batch:
    backend:
      type: KAFKA
      kafka:
        bootstrap-servers: fluffy-kafka-kafka-bootstrap.fluffy.svc:9092
        topic: fluffy-jobs
        group-id: fluffy-batch

spring:
  datasource:
    url: jdbc:postgresql://postgresql:5432/fluffydb
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
```

---

## Option 3 — Using OpenShift Templates

Create a reusable OpenShift template for easy deployment:

```yaml
apiVersion: template.openshift.io/v1
kind: Template
metadata:
  name: fluffy-batch
  annotations:
    description: "Fluffy Batch Starter deployment template"
    tags: "java,spring-boot,batch"
parameters:
  - name: APP_NAME
    value: fluffy-batch
  - name: IMAGE
    required: true
  - name: REPLICAS
    value: "2"
  - name: BACKEND_TYPE
    value: "DATABASE"
  - name: POSTGRES_HOST
    required: true
  - name: POSTGRES_PORT
    value: "5432"
  - name: POSTGRES_DB
    value: fluffydb
  - name: POSTGRES_USER
    required: true
  - name: POSTGRES_PASSWORD
    required: true
objects:
  - apiVersion: v1
    kind: Secret
    metadata:
      name: ${APP_NAME}-db
    stringData:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
  - apiVersion: apps/v1
    kind: Deployment
    metadata:
      name: ${APP_NAME}
      labels:
        app: ${APP_NAME}
    spec:
      replicas: ${{REPLICAS}}
      selector:
        matchLabels:
          app: ${APP_NAME}
      template:
        metadata:
          labels:
            app: ${APP_NAME}
        spec:
          containers:
            - name: ${APP_NAME}
              image: ${IMAGE}
              ports:
                - containerPort: 8080
              env:
                - name: SPRING_PROFILES_ACTIVE
                  value: "database"
                - name: POSTGRES_HOST
                  value: ${POSTGRES_HOST}
                - name: POSTGRES_PORT
                  value: ${POSTGRES_PORT}
              envFrom:
                - secretRef:
                    name: ${APP_NAME}-db
              readinessProbe:
                httpGet:
                  path: /api/jobs/registered
                  port: 8080
                periodSeconds: 10
              resources:
                requests:
                  memory: "320Mi"
                  cpu: "200m"
                limits:
                  memory: "640Mi"
                  cpu: "1000m"
  - apiVersion: v1
    kind: Service
    metadata:
      name: ${APP_NAME}
    spec:
      selector:
        app: ${APP_NAME}
      ports:
        - port: 8080
          targetPort: 8080
  - apiVersion: route.openshift.io/v1
    kind: Route
    metadata:
      name: ${APP_NAME}
    spec:
      to:
        kind: Service
        name: ${APP_NAME}
      tls:
        termination: edge
```

Process and apply the template:

```bash
oc process -f fluffy-template.yaml \
  -p IMAGE=image-registry.openshift-image-registry.svc:5000/fluffy/fluffy-batch-example:latest \
  -p POSTGRES_HOST=postgresql \
  -p POSTGRES_USER=fluffy \
  -p POSTGRES_PASSWORD=<SECURE_PASSWORD> | \
  oc apply -n fluffy -f -
```

---

## Autoscaling

### HorizontalPodAutoscaler

```bash
oc apply -f k8s/hpa.yaml -n fluffy
```

### KEDA on OpenShift

Install the Custom Metrics Autoscaler Operator (KEDA) from OperatorHub:

```bash
cat <<EOF | oc apply -f -
apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: openshift-custom-metrics-autoscaler-operator
  namespace: openshift-keda
spec:
  channel: stable
  name: openshift-custom-metrics-autoscaler-operator
  source: redhat-operators
  sourceNamespace: openshift-marketplace
EOF
```

Then create a `ScaledObject` for queue-depth-based scaling. See the [Scaling Configuration](scaling.md) guide.

---

## Monitoring

### OpenShift Monitoring Stack

OpenShift ships with a built-in Prometheus-based monitoring stack. Enable user workload monitoring:

```bash
oc apply -f - <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: cluster-monitoring-config
  namespace: openshift-monitoring
data:
  config.yaml: |
    enableUserWorkload: true
EOF
```

Create a `ServiceMonitor` to scrape Fluffy metrics:

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: fluffy-monitor
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

| Metric | Alert Suggestion |
|---|---|
| `fluffy_batch_queue_depth` | > 50 for 5 minutes |
| `fluffy_batch_jobs_active` | Near max concurrency for 10 minutes |
| `fluffy_batch_jobs_failed_total` | Rate > 0.1/s for 5 minutes |

---

## Security Best Practices

- Use **OpenShift Secrets** or integrate with **HashiCorp Vault** via the Vault CSI Provider for credential management.
- Apply **Security Context Constraints (SCCs)** — the Fluffy container runs as non-root by default, which is compatible with OpenShift's `restricted-v2` SCC.
- Use **NetworkPolicies** to restrict traffic between pods — allow only Fluffy pods to reach PostgreSQL and Kafka services.
- Enable **TLS on Routes** for external access (edge or re-encrypt termination).
- Use **OpenShift OAuth** or integrate with your identity provider for dashboard access control.

### Security Context

The Fluffy Docker image already runs as a non-root user (`fluffy`), which is compatible with OpenShift's default security policy. No special SCC is required:

```dockerfile
RUN addgroup -S fluffy && adduser -S fluffy -G fluffy
USER fluffy
```

---

## Cost Optimization

| Component | Tip |
|---|---|
| Worker nodes | Use machine autoscaler to scale down idle nodes |
| PostgreSQL | Use Crunchy Operator for automated failover and backup |
| AMQ Streams | Start with 3 brokers; scale based on consumer lag |
| Monitoring | Use built-in OpenShift monitoring — no additional cost |

---

## Further Reading

- [Backend Configuration](backends.md) — Choosing H2, Database, or Kafka mode
- [Scaling Configuration](scaling.md) — HPA and KEDA autoscaling examples
- [Fault Tolerance & Recovery](recovery.md) — Node heartbeat and job recovery
- [Metrics Reference](metrics.md) — Prometheus metrics and Grafana queries
