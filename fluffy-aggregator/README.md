# Fluffy Aggregator

The **Fluffy Aggregator** is an optional layer built into `fluffy-batch-starter`
that collects job metrics from multiple Fluffy nodes and presents a unified,
React + Material-UI dashboard.

It is **not** a separate application — it is a feature of the starter that you
enable with a single configuration property.  Any Fluffy node can act as the
aggregator alongside its own job processing, or you can run a dedicated
aggregator-only pod.

---

## How It Works

```
┌─────────────────────────┐
│     Aggregator Node     │
│  (any Fluffy instance)  │──▶ polls GET /api/jobs/summary
│                         │
│  /api/aggregator/*      │         ┌──────────┐  ┌──────────┐  ┌──────────┐
│  /fluffy-aggregator     │         │  Node 1  │  │  Node 2  │  │  Node N  │
└─────────────────────────┘         └──────────┘  └──────────┘  └──────────┘
```

Every Fluffy node automatically exposes `GET /api/jobs/summary`.  The
aggregator periodically polls that endpoint on every configured node, merges
the results, and serves a React dashboard at `/fluffy-aggregator`.

---

## Enabling the Aggregator

Add the following to `application.yml` (or the equivalent environment
variables):

```yaml
fluffy:
  batch:
    aggregator:
      enabled: true
      title: "My Aggregator Dashboard"   # optional, defaults to 'Fluffy Aggregator Dashboard'
      nodes:
        - http://node1:8080
        - http://node2:8080
      poll-interval-seconds: 10          # how often to poll nodes (default: 10)
      discovery-interval-seconds: 30     # how often to refresh the node list (default: 30)
```

Equivalent environment variables:

```
FLUFFY_BATCH_AGGREGATOR_ENABLED=true
FLUFFY_BATCH_AGGREGATOR_NODES_0_=http://node1:8080
FLUFFY_BATCH_AGGREGATOR_NODES_1_=http://node2:8080
FLUFFY_BATCH_AGGREGATOR_POLL_INTERVAL_SECONDS=10
```

---

## REST API

| Endpoint | Description |
|---|---|
| `GET /api/aggregator/summary` | Aggregated metrics + per-node breakdown |
| `GET /api/aggregator/nodes` | Per-node summaries only |
| `GET /api/aggregator/refresh` | Trigger an immediate re-poll, return summary |
| `GET /api/aggregator/config` | Current aggregator configuration |

### Example — `/api/aggregator/summary`

```json
{
  "totalQueued": 8,
  "totalRunning": 6,
  "totalSucceeded": 240,
  "totalFailed": 4,
  "nodeSummaries": [
    {
      "nodeId": "node-1",
      "queued": 5, "running": 3, "succeeded": 120, "failed": 2,
      "dashboardAvailable": true,
      "dashboardUrl": "http://node1:8080/fluffy-dashboard"
    },
    {
      "nodeId": "node-2",
      "queued": 3, "running": 3, "succeeded": 120, "failed": 2,
      "dashboardAvailable": false,
      "dashboardUrl": null
    }
  ]
}
```

---

## Dashboard UI

Access the React + MUI dashboard at:

```
http://<aggregator-host>:<port>/fluffy-aggregator
```

**Features:**
- Aggregated metric cards (queued, running, succeeded, failed) across all nodes.
- Per-node breakdown table with live status.
- "Open" buttons linking directly to each node's own Fluffy dashboard.
- Auto-refresh on the configured poll interval.
- Manual refresh button for immediate re-poll.

The UI ships as a single HTML page (`fluffy-aggregator/index.html`) plus a
vanilla-JS file.  No build step is required.

---

## Running Locally (Development)

The simplest way to see the aggregator in action is to run two instances of the
example app on different ports, then enable the aggregator on a third:

```bash
# Terminal 1 — Node 1
cd fluffy-batch-starter/fluffy-batch-example
SERVER_PORT=8081 mvn spring-boot:run

# Terminal 2 — Node 2
SERVER_PORT=8082 mvn spring-boot:run

# Terminal 3 — Aggregator
SERVER_PORT=8080 mvn spring-boot:run \
  --fluffy.batch.aggregator.enabled=true \
  --fluffy.batch.aggregator.nodes=http://localhost:8081,http://localhost:8082
```

Then open `http://localhost:8080/fluffy-aggregator`.

---

## Kubernetes Deployment

The example Kubernetes manifest for the aggregator pod is located at:

```
fluffy-batch-starter/fluffy-batch-example/k8s/app-aggregator.yaml
```

It runs the same `fluffy-batch-example` Docker image with the aggregator
enabled via environment variables:

```yaml
env:
  - name: FLUFFY_BATCH_AGGREGATOR_ENABLED
    value: "true"
  - name: FLUFFY_BATCH_AGGREGATOR_NODES_0_
    value: "http://fluffy-batch-example:8080"
  - name: FLUFFY_BATCH_AGGREGATOR_NODES_1_
    value: "http://fluffy-batch-h2:8080"
  - name: FLUFFY_BATCH_AGGREGATOR_NODES_2_
    value: "http://fluffy-batch-db:8080"
  - name: FLUFFY_BATCH_AGGREGATOR_NODES_3_
    value: "http://fluffy-batch-kafka:8080"
```

The setup scripts (`setup-and-deploy.sh` / `setup-and-deploy.ps1`) deploy this
manifest automatically as part of the full example deployment.  The aggregator
is accessible on NodePort **30084** after deployment:

```
http://<minikube-ip>:30084/fluffy-aggregator
```

### Using a Dedicated Aggregator Pod

To run the aggregator as a completely separate pod (no job processing on that
node), set `fluffy.batch.dashboard.enabled=false` in addition to enabling the
aggregator.  This makes the pod purely an aggregation/display layer.

---

## Configuration Reference

| Property | Default | Description |
|---|---|---|
| `fluffy.batch.aggregator.enabled` | `false` | Enable the aggregator |
| `fluffy.batch.aggregator.title` | `"Fluffy Aggregator Dashboard"` | Dashboard title |
| `fluffy.batch.aggregator.nodes` | `[]` | Static list of Fluffy node base URLs |
| `fluffy.batch.aggregator.poll-interval-seconds` | `10` | Node polling frequency |
| `fluffy.batch.aggregator.discovery-interval-seconds` | `30` | Node list refresh frequency |
| `fluffy.batch.aggregator.dashboard-path` | `/fluffy-aggregator` | URL path for the dashboard |

---

## Source Code Location

The aggregator implementation lives inside `fluffy-batch-starter` because it is
an auto-configured feature of the starter library:

```
fluffy-batch-starter/src/main/java/com/fluffy/batch/aggregator/
  AggregatorAutoConfiguration.java   ← @ConditionalOnProperty wiring
  AggregatorController.java          ← REST endpoints
  AggregatorService.java             ← polling & aggregation logic
  NodeDiscoveryService.java          ← node list management
  AggregatorProperties.java          ← configuration properties
  AggregatedSummary.java             ← response record
  AggregatorDashboardRedirect.java   ← redirects /fluffy-aggregator → index.html

fluffy-batch-starter/src/main/resources/static/fluffy-aggregator/
  index.html                         ← React/MUI entry point
  js/aggregator-app.js               ← all UI logic (no build step)
```

---

## Limitations

- **Static node list** — nodes must be listed in configuration.  Dynamic
  service-registry discovery is a planned enhancement.
- **Eventual consistency** — the aggregated view lags real time by up to
  `poll-interval-seconds`.
- **Single aggregator** — no built-in HA.  Use a Kubernetes Deployment with
  replicas + a load balancer for resilience.

---

## Further Reading

- [Aggregator deep-dive](../docs/aggregator.md)
- [Dashboard feature](../docs/dashboard.md)
- [Architecture overview](../docs/architecture.md)
