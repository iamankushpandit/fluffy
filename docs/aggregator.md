# Aggregator Dashboard

Fluffy Batch Starter includes an optional **aggregator layer** that collects job
metrics from multiple nodes and presents a unified, React-based dashboard.

## Architecture

```
┌─────────────────────┐
│  Aggregator Node    │
│  (React / MUI UI)   │──▶ GET /api/jobs/summary
│                     │         │
│  /api/aggregator/*  │         ▼
│  /fluffy-aggregator │   ┌──────────┐  ┌──────────┐  ┌──────────┐
└─────────────────────┘   │  Node 1  │  │  Node 2  │  │  Node N  │
                          │ :8081    │  │ :8082    │  │ :808N    │
                          └──────────┘  └──────────┘  └──────────┘
```

Each Fluffy node exposes a lightweight **summary endpoint**
(`GET /api/jobs/summary`) that returns job counts and a dashboard availability
flag.  The aggregator periodically discovers nodes, polls their summaries, and
serves a React + Material-UI dashboard at `/fluffy-aggregator`.

---

## Enabling the Aggregator

Add the following to your `application.yml`:

```yaml
fluffy:
  batch:
    aggregator:
      enabled: true
      nodes:
        - http://node1:8080
        - http://node2:8080
```

The aggregator is **disabled by default** (`enabled: false`).

---

## Node Summary Endpoint

Every Fluffy node automatically exposes:

```
GET /api/jobs/summary
```

### Response

```json
{
  "nodeId": "fluffy-node-abc123",
  "queued": 5,
  "running": 3,
  "succeeded": 120,
  "failed": 2,
  "dashboardAvailable": true,
  "dashboardUrl": "http://node1:8080/fluffy-dashboard"
}
```

| Field                | Description                                        |
|----------------------|----------------------------------------------------|
| `nodeId`             | Hostname or `fluffy.node.id` system property        |
| `queued`             | Jobs in `STARTING` status                           |
| `running`            | Jobs in `STARTED` status                            |
| `succeeded`          | Jobs in `COMPLETED` status                          |
| `failed`             | Jobs in `FAILED` status                             |
| `dashboardAvailable` | `true` when the per-node dashboard is enabled        |
| `dashboardUrl`       | Full URL to the per-node dashboard (or `null`)       |

---

## Aggregator REST API

When the aggregator is enabled, the following endpoints are available:

### `GET /api/aggregator/summary`

Returns aggregated metrics plus per-node breakdowns.

```json
{
  "totalQueued": 8,
  "totalRunning": 6,
  "totalSucceeded": 240,
  "totalFailed": 4,
  "nodeSummaries": [
    { "nodeId": "node-1", "queued": 5, "running": 3, "succeeded": 120, "failed": 2, "dashboardAvailable": true, "dashboardUrl": "http://node1:8080/fluffy-dashboard" },
    { "nodeId": "node-2", "queued": 3, "running": 3, "succeeded": 120, "failed": 2, "dashboardAvailable": false, "dashboardUrl": null }
  ]
}
```

### `GET /api/aggregator/nodes`

Returns per-node summaries only (same as `nodeSummaries` array above).

### `GET /api/aggregator/refresh`

Triggers an immediate node discovery refresh and poll, then returns the
aggregated summary.

### `GET /api/aggregator/config`

Returns the current aggregator configuration:

```json
{
  "pollIntervalSeconds": 10,
  "discoveryIntervalSeconds": 30,
  "nodeCount": 2
}
```

---

## Configuration Reference

| Property                                          | Default              | Description                                   |
|---------------------------------------------------|----------------------|-----------------------------------------------|
| `fluffy.batch.aggregator.enabled`                 | `false`              | Enable the aggregator layer                   |
| `fluffy.batch.aggregator.nodes`                   | `[]`                 | Static list of node base URLs                 |
| `fluffy.batch.aggregator.poll-interval-seconds`   | `10`                 | Seconds between polling node summaries        |
| `fluffy.batch.aggregator.discovery-interval-seconds` | `30`              | Seconds between refreshing the node list      |
| `fluffy.batch.aggregator.dashboard-path`          | `/fluffy-aggregator` | URL path for the aggregator React dashboard   |

---

## React Dashboard

The aggregator dashboard is a React + Material-UI application served as static
resources.  Access it at:

```
http://aggregator-host:port/fluffy-aggregator/index.html
```

### Features

- **Aggregated metric cards** — total queued, running, succeeded, and failed
  counts across all discovered nodes.
- **Per-node breakdown table** — individual node metrics with status columns.
- **Dashboard links** — each row shows an "Open" button linking to the node's
  own dashboard when available, or "N/A" when not.
- **Auto-refresh** — the UI polls `/api/aggregator/summary` at the configured
  interval.
- **Manual refresh** — click the **Refresh** toolbar button to trigger an
  immediate re-discovery and re-poll.

### Technology

| Library     | Version | Loaded via CDN                       |
|-------------|---------|--------------------------------------|
| React       | 18      | `unpkg.com/react@18`                 |
| ReactDOM    | 18      | `unpkg.com/react-dom@18`             |
| Material-UI | 5       | `unpkg.com/@mui/material@5`          |
| Roboto font | —       | `fonts.googleapis.com`               |

No build step is required; the dashboard ships as a single HTML page plus a
vanilla JS file that uses `React.createElement` calls.

---

## Example Setup

### Single Machine (Development)

Run two instances of the example application on different ports:

```bash
# Node 1
SERVER_PORT=8081 java -jar fluffy-batch-example.jar

# Node 2
SERVER_PORT=8082 java -jar fluffy-batch-example.jar

# Aggregator (can also be a third instance)
SERVER_PORT=8080 java -jar fluffy-batch-example.jar \
  --fluffy.batch.aggregator.enabled=true \
  --fluffy.batch.aggregator.nodes=http://localhost:8081,http://localhost:8082
```

### Kubernetes

Add the aggregator configuration to your Deployment or ConfigMap:

```yaml
env:
  - name: FLUFFY_BATCH_AGGREGATOR_ENABLED
    value: "true"
  - name: FLUFFY_BATCH_AGGREGATOR_NODES
    value: "http://fluffy-node-0.fluffy:8080,http://fluffy-node-1.fluffy:8080"
```

Or use Spring Cloud Kubernetes for dynamic service discovery.

---

## Limitations

- **Static node list** — nodes must be listed in configuration.  DNS-based or
  service-registry discovery is a planned enhancement.
- **Eventual consistency** — the aggregated view is refreshed at the configured
  poll interval and may lag real-time state by a few seconds.
- **Single aggregator** — there is no built-in HA for the aggregator itself.
  Use a load balancer or Kubernetes Deployment with replicas for resilience.
