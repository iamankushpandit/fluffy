# Deploying Fluffy on AWS

This guide covers common patterns for running Fluffy Batch Starter on Amazon Web Services.

---

## Architecture Overview

```
                         ┌──────────────────────┐
                         │   Application Load    │
                         │      Balancer (ALB)   │
                         └──────────┬───────────┘
                                    │
            ┌───────────────────────┼───────────────────────┐
            ▼                       ▼                       ▼
   ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
   │   Fluffy Node 1 │   │   Fluffy Node 2 │   │   Fluffy Node N │
   │   (ECS / EKS)   │   │   (ECS / EKS)   │   │   (ECS / EKS)   │
   └────────┬────────┘   └────────┬────────┘   └────────┬────────┘
            │                     │                     │
            └─────────────────────┼─────────────────────┘
                                  │
                    ┌─────────────┴─────────────┐
                    ▼                           ▼
          ┌──────────────────┐       ┌──────────────────┐
          │   Amazon RDS     │       │   Amazon MSK     │
          │  (PostgreSQL)    │       │   (Kafka)        │
          └──────────────────┘       └──────────────────┘
```

---

## Option 1 — Amazon EKS (Elastic Kubernetes Service)

EKS is the most natural fit because Fluffy ships with Kubernetes manifests.

### Prerequisites

- AWS CLI configured with appropriate IAM permissions
- `eksctl` and `kubectl` installed
- An ECR repository for the Docker image

### 1. Create the EKS Cluster

```bash
eksctl create cluster \
  --name fluffy-cluster \
  --region us-east-1 \
  --nodegroup-name workers \
  --node-type t3.medium \
  --nodes 3 \
  --managed
```

### 2. Build and Push the Docker Image

```bash
# Authenticate to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com

# Build
cd fluffy-batch-starter/fluffy-batch-example
docker build -t fluffy-batch-example:latest .

# Tag and push
docker tag fluffy-batch-example:latest \
  <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/fluffy-batch-example:latest
docker push <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/fluffy-batch-example:latest
```

### 3. Provision Amazon RDS (PostgreSQL)

```bash
aws rds create-db-instance \
  --db-instance-identifier fluffy-db \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 16 \
  --master-username fluffy \
  --master-user-password <SECURE_PASSWORD> \
  --allocated-storage 20 \
  --vpc-security-group-ids <SG_ID>
```

Store the credentials in a Kubernetes Secret:

```bash
kubectl create namespace fluffy

kubectl create secret generic postgres-secret \
  -n fluffy \
  --from-literal=POSTGRES_DB=fluffydb \
  --from-literal=POSTGRES_USER=fluffy \
  --from-literal=POSTGRES_PASSWORD=<SECURE_PASSWORD>
```

### 4. Update Kubernetes Manifests

Modify `k8s/app-db.yaml` to point at the RDS endpoint and use the ECR image:

```yaml
containers:
  - name: fluffy-batch-db
    image: <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/fluffy-batch-example:latest
    imagePullPolicy: Always
    env:
      - name: SPRING_PROFILES_ACTIVE
        value: "database"
      - name: POSTGRES_HOST
        value: "fluffy-db.<HASH>.us-east-1.rds.amazonaws.com"
      - name: POSTGRES_PORT
        value: "5432"
```

### 5. Deploy

```bash
kubectl apply -f k8s/app-db.yaml -n fluffy
kubectl rollout status deployment/fluffy-batch-db -n fluffy --timeout=180s
```

### 6. Autoscaling with HPA

```bash
kubectl apply -f k8s/hpa.yaml -n fluffy
```

For queue-depth-based scaling, install the [Prometheus Adapter](https://github.com/kubernetes-sigs/prometheus-adapter) or [KEDA](https://keda.sh/). See the [Scaling Configuration](scaling.md) guide.

---

## Option 2 — Amazon ECS with Fargate

Fargate removes the need to manage EC2 instances.

### 1. Create an ECS Cluster

```bash
aws ecs create-cluster --cluster-name fluffy-cluster --capacity-providers FARGATE
```

### 2. Task Definition

Create `fluffy-task-def.json`:

```json
{
  "family": "fluffy-batch",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::<ACCOUNT_ID>:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "fluffy-batch",
      "image": "<ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/fluffy-batch-example:latest",
      "portMappings": [
        { "containerPort": 8080, "protocol": "tcp" }
      ],
      "environment": [
        { "name": "SPRING_PROFILES_ACTIVE", "value": "database" },
        { "name": "POSTGRES_HOST", "value": "fluffy-db.<HASH>.us-east-1.rds.amazonaws.com" },
        { "name": "POSTGRES_PORT", "value": "5432" }
      ],
      "secrets": [
        {
          "name": "POSTGRES_USER",
          "valueFrom": "arn:aws:secretsmanager:us-east-1:<ACCOUNT_ID>:secret:fluffy-db-creds:username::"
        },
        {
          "name": "POSTGRES_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:us-east-1:<ACCOUNT_ID>:secret:fluffy-db-creds:password::"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/fluffy-batch",
          "awslogs-region": "us-east-1",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

```bash
aws ecs register-task-definition --cli-input-json file://fluffy-task-def.json
```

### 3. Create the Service

```bash
aws ecs create-service \
  --cluster fluffy-cluster \
  --service-name fluffy-batch \
  --task-definition fluffy-batch \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[<SUBNET_IDS>],securityGroups=[<SG_ID>],assignPublicIp=ENABLED}"
```

### 4. ECS Auto Scaling

```bash
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --resource-id service/fluffy-cluster/fluffy-batch \
  --scalable-dimension ecs:service:DesiredCount \
  --min-capacity 1 \
  --max-capacity 10

aws application-autoscaling put-scaling-policy \
  --service-namespace ecs \
  --resource-id service/fluffy-cluster/fluffy-batch \
  --scalable-dimension ecs:service:DesiredCount \
  --policy-name fluffy-cpu-scaling \
  --policy-type TargetTrackingScaling \
  --target-tracking-scaling-policy-configuration \
    "TargetValue=70.0,PredefinedMetricSpecification={PredefinedMetricType=ECSServiceAverageCPUUtilization}"
```

---

## Option 3 — Kafka Backend with Amazon MSK

When high-throughput job ingestion is required, use Amazon MSK as the Kafka backend.

### 1. Create an MSK Cluster

```bash
aws kafka create-cluster \
  --cluster-name fluffy-kafka \
  --broker-node-group-info \
    "InstanceType=kafka.t3.small,ClientSubnets=<SUBNET_IDS>,SecurityGroups=<SG_ID>" \
  --kafka-version 3.7.0 \
  --number-of-broker-nodes 3
```

### 2. Create the Topic

```bash
aws kafka create-topic \
  --cluster-arn <MSK_CLUSTER_ARN> \
  --topic-name fluffy-jobs \
  --partitions 6 \
  --replication-factor 3
```

### 3. Application Configuration

```yaml
fluffy:
  batch:
    backend:
      type: KAFKA
      kafka:
        bootstrap-servers: b-1.fluffy-kafka.<HASH>.kafka.us-east-1.amazonaws.com:9092
        topic: fluffy-jobs
        group-id: fluffy-batch

spring:
  datasource:
    url: jdbc:postgresql://fluffy-db.<HASH>.us-east-1.rds.amazonaws.com:5432/fluffydb
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
```

---

## Monitoring

### CloudWatch

Fluffy exposes Micrometer metrics via the `/actuator/prometheus` endpoint. Use the [CloudWatch Agent](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/ContainerInsights-Prometheus.html) or [AWS Distro for OpenTelemetry](https://aws-otel.github.io/) to forward Prometheus metrics to CloudWatch.

Key metrics to monitor:

| Metric | CloudWatch Alarm Suggestion |
|---|---|
| `fluffy_batch_queue_depth` | > 50 for 5 minutes |
| `fluffy_batch_jobs_active` | Near max concurrency for 10 minutes |
| `fluffy_batch_jobs_failed_total` | Rate > 0.1/s for 5 minutes |

### X-Ray Tracing

Add the X-Ray SDK dependency and enable tracing for end-to-end visibility across distributed job execution.

---

## Security Best Practices

- Store database and Kafka credentials in **AWS Secrets Manager** and reference them in ECS task definitions or EKS Secrets.
- Place RDS and MSK in **private subnets** — use VPC security groups to restrict access to the application layer only.
- Enable **RDS encryption at rest** and **MSK TLS** for data in transit.
- Use **IAM Roles for Service Accounts (IRSA)** in EKS to grant fine-grained AWS permissions to pods.
- Enable **RDS automated backups** and configure point-in-time recovery.

---

## Cost Optimization

| Component | Cost Tip |
|---|---|
| EKS nodes | Use Spot instances for non-critical worker nodes |
| Fargate | Use Fargate Spot for batch workloads that tolerate interruption |
| RDS | Use `db.t3.micro` for dev; scale to `db.r6g` for production |
| MSK | Use `kafka.t3.small` for dev; `kafka.m5.large` for production |

---

## Further Reading

- [Backend Configuration](backends.md) — Choosing H2, Database, or Kafka mode
- [Scaling Configuration](scaling.md) — HPA and KEDA autoscaling examples
- [Fault Tolerance & Recovery](recovery.md) — Node heartbeat and job recovery
- [Metrics Reference](metrics.md) — Prometheus metrics and Grafana queries
