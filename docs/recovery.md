# Fault Tolerance & Recovery

Fluffy Batch Starter includes a node heartbeat and job recovery mechanism for multi-node deployments. Recovery is available in **Database** and **Kafka** backend modes only.

## Enabling Recovery

```yaml
fluffy:
  batch:
    backend:
      type: DATABASE  # or KAFKA
    recovery:
      enabled: true
      heartbeat-interval: 15
      stale-threshold: 60
```

Recovery is ignored when the backend type is `H2`.

## Node Heartbeat Mechanism

Each application instance periodically writes a heartbeat record to the `node_heartbeat` table. The heartbeat contains:

- **Node ID** — unique identifier for the instance
- **Last heartbeat timestamp** — updated every `heartbeat-interval` seconds

This allows the cluster to detect which nodes are alive.

## Stale Node Detection

A node is considered **stale** when its last heartbeat is older than `stale-threshold` seconds. The recovery manager periodically scans for stale nodes and triggers job recovery for any jobs owned by those nodes.

**Timeline example** (defaults):

```
t=0s    Node-A writes heartbeat
t=15s   Node-A writes heartbeat
t=30s   Node-A crashes
t=45s   Node-A misses heartbeat (no write)
t=60s   Node-A misses heartbeat — still within threshold
t=90s   stale-threshold exceeded — Node-A marked stale
        Recovery triggered for Node-A's jobs
```

## Job Recovery Process

When a stale node is detected:

1. **Find orphaned jobs** — query `job_execution` for rows with `owner_node = <stale-node>` and status `STARTED` or `IN_PROGRESS`.
2. **Re-queue jobs** — each orphaned execution is set back to `IN_QUEUE` and placed into the queue backend.
3. **Clean up heartbeat** — the stale node's heartbeat record is removed.
4. **Normal processing resumes** — healthy nodes pick up the re-queued jobs through standard polling.

Jobs that were `IN_QUEUE` on the stale node are also recovered since the queue backend entry may reference the dead node.

## Configuration Reference

| Property | Default | Description |
|---|---|---|
| `fluffy.batch.recovery.enabled` | `false` | Enable heartbeat and recovery |
| `fluffy.batch.recovery.heartbeat-interval` | `15` | Seconds between heartbeat writes |
| `fluffy.batch.recovery.stale-threshold` | `60` | Seconds before a node is considered stale |

## Recommendations

- Set `stale-threshold` to at least **3–4×** the `heartbeat-interval` to avoid false positives caused by GC pauses or network blips.
- In Kubernetes, combine recovery with a `readinessProbe` so that traffic stops reaching a node before it is marked stale.
- Use the `fluffy.batch.jobs.active` metric to monitor recovered jobs across the cluster.
