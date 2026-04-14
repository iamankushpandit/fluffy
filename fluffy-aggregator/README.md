# Fluffy Aggregator Service

A lightweight, standalone **Spring Boot service** that aggregates job metrics
from multiple [Fluffy](https://github.com/iamankushpandit/fluffy) nodes and
presents them in a single React + Material-UI dashboard.

---

## Why a Standalone Service?

The aggregator has a fundamentally different role from the batch nodes:

- Batch nodes **process jobs** — they run `fluffy-batch-starter`.
- The aggregator **observes nodes** — it polls them, merges results, and serves
  a unified dashboard.

Because these responsibilities are distinct, the aggregator is its own
deployable unit with its own `Dockerfile`, Kubernetes manifests, Maven module,
and configuration namespace (`fluffy.aggregator.*`).

---

## How It Works

```
┌──────────────────────────┐
│   fluffy-aggregator      │
│   (standalone service)   │──▶ GET /api/jobs/summary
│                          │         │
│   Port 8084 by default   │         ▼
│   /api/aggregator/*      │   ┌──────────┐  ┌──────────┐  ┌──────────┐
│   /fluffy-aggregator     │   │  Node 1  │  │  Node 2  │  │  Node N  │
└──────────────────────────┘   └──────────┘  └──────────┘  └──────────┘
```

Every Fluffy batch node automatically exposes `GET /api/jobs/summary`.
The aggregator polls each configured node, caches the results, and serves:

- `GET /api/aggregator/summary` — aggregated + per-node breakdown
- `GET /api/aggregator/nodes` — per-node summaries
- `GET /api/aggregator/refresh` — force immediate re-poll
- `GET /api/aggregator/config` — current configuration (used by the UI)
- `/fluffy-aggregator/` — React + MUI dashboard

---

## Quick Start

### Build

```bash
# From the repository root:
mvn clean package -pl fluffy-aggregator -DskipTests
```

### Run

```bash
cd fluffy-aggregator
java -jar target/fluffy-aggregator-*.jar \
  --fluffy.aggregator.nodes=http://localhost:8081,http://localhost:8082
```

Open the dashboard: `http://localhost:8084/fluffy-aggregator`

### Development (with live Fluffy nodes)

```bash
# Terminal 1 — Fluffy node 1
cd fluffy-batch-starter/fluffy-batch-example
SERVER_PORT=8081 mvn spring-boot:run

# Terminal 2 — Fluffy node 2
SERVER_PORT=8082 mvn spring-boot:run

# Terminal 3 — Aggregator
cd fluffy-aggregator
mvn spring-boot:run \
  --spring-boot.run.arguments="--fluffy.aggregator.nodes=http://localhost:8081,http://localhost:8082"
```

---

## Configuration

`src/main/resources/application.yml`:

```yaml
server:
  port: 8084

fluffy:
  aggregator:
    nodes:
      - http://node1:8080
      - http://node2:8080
    poll-interval-seconds: 10
    discovery-interval-seconds: 30
    title: "My Aggregator Dashboard"
    # Rewrite internal K8s URLs to browser-accessible URLs for dashboard links:
    # external-url-mappings: "http://svc1:8080=http://localhost:8081,http://svc2:8080=http://localhost:8082"
```

### Full Property Reference

| Property | Default | Description |
|---|---|---|
| `fluffy.aggregator.nodes` | `[]` | List of Fluffy node base URLs |
| `fluffy.aggregator.poll-interval-seconds` | `10` | Polling frequency |
| `fluffy.aggregator.discovery-interval-seconds` | `30` | Node list refresh frequency |
| `fluffy.aggregator.title` | `"Fluffy Aggregator Dashboard"` | Dashboard title |
| `fluffy.aggregator.dashboard-path` | `/fluffy-aggregator` | Dashboard URL path |
| `fluffy.aggregator.external-url-mappings` | `""` | `internal=external` URL pairs |

---

## Docker

```bash
# Build
cd fluffy-aggregator
docker build -t fluffy-aggregator:latest .

# Run
docker run -p 8084:8084 \
  -e FLUFFY_AGGREGATOR_NODES_0_=http://node1:8080 \
  -e FLUFFY_AGGREGATOR_NODES_1_=http://node2:8080 \
  fluffy-aggregator:latest
```

---

## Kubernetes

The `k8s/app-aggregator.yaml` manifest deploys the aggregator in the
`fluffy-example-1` namespace and exposes it on NodePort **30084**.

```bash
kubectl apply -f fluffy-aggregator/k8s/app-aggregator.yaml -n fluffy-example-1
```

The setup scripts (`setup-and-deploy.sh` / `setup-and-deploy.ps1`) build the
Docker image and apply this manifest automatically.

---

## Module Structure

```
fluffy-aggregator/
  pom.xml                         ← Maven module (Spring Boot app)
  Dockerfile                      ← Container image definition
  README.md                       ← This file
  k8s/
    app-aggregator.yaml           ← Kubernetes Deployment + Service
  src/
    main/
      java/com/fluffy/aggregator/
        FluffyAggregatorApplication.java
        AggregatorController.java ← REST API
        AggregatorService.java    ← Polling & aggregation logic
        NodeDiscoveryService.java ← Node list management
        AggregatorProperties.java ← Configuration properties
        AggregatorScheduler.java  ← @Scheduled polling & discovery
        AggregatorWebConfig.java  ← RestClient, redirect filter, resource handler
        AggregatedSummary.java    ← Aggregated response record
        NodeSummary.java          ← Per-node response record (mirrors Fluffy API)
      resources/
        application.yml
        static/fluffy-aggregator/ ← React + MUI dashboard (no build step)
          index.html
          js/aggregator-app.js
    test/
      java/com/fluffy/aggregator/ ← Unit + integration tests (90% coverage)
```

---

## Further Reading

- [Aggregator deep-dive](../docs/aggregator.md)
- [Architecture overview](../docs/architecture.md)
- [Developer Guide](../docs/developer-guide.md)
