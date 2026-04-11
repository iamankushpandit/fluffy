# Fluffy

[![CI](https://github.com/iamankushpandit/fluffy/actions/workflows/ci.yml/badge.svg)](https://github.com/iamankushpandit/fluffy/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4-green.svg)](https://spring.io/projects/spring-boot)

A lightweight Spring Boot starter for batch job processing. Fluffy provides a simple, annotation-driven API to define, launch, and monitor batch jobs with built-in concurrency management, retry support, and a REST API.

## Backend Modes

Fluffy supports three pluggable backend modes — switch between them with a single property:

| Mode | Description | Best For |
|---|---|---|
| **H2** (default) | In-memory queue and coordination | Development, testing, single-node |
| **Database** | PostgreSQL-backed queue with shared coordination | Multi-node with persistence |
| **Kafka** | Kafka topic queue with database coordination | High-throughput distributed deployments |

```yaml
fluffy:
  batch:
    backend:
      type: DATABASE  # H2 | DATABASE | KAFKA
```

See the [Backend Configuration Guide](docs/backends.md) for full details and migration instructions.

## Modules

| Module | Description |
|---|---|
| `fluffy-batch-starter` | Core library — Spring Boot auto-configuration, job engine, REST API |
| `fluffy-batch-example` | Example application demonstrating usage |

## When to Use Fluffy

Fluffy is a good fit when you need:

- **Simple, in-process batch jobs** triggered via REST API — report generation, data sync, file processing, or scheduled clean-up tasks.
- **Per-job concurrency control** — limit how many instances of a job run at the same time and automatically queue the rest.
- **Multi-node coordination** — use the Database or Kafka backend for shared queue and concurrency state across instances.
- **Lightweight coordination** without a full-blown workflow engine — just annotate a class, and the starter gives you launch, stop, retry, and status endpoints.
- **Virtual-thread-powered execution** — ideal for I/O-bound jobs (database queries, HTTP calls, file transfers) that benefit from Java 21 virtual threads.
- **Kubernetes-ready scaling** — built-in metrics, scaling APIs, and support for HPA / KEDA autoscaling.
- **Spring Boot auto-configuration** — drop the starter into any Spring Boot app and get a complete job framework with zero boilerplate.

## When **Not** to Use Fluffy

Fluffy is intentionally simple. Consider alternatives when you need:

- **Complex DAG / multi-step pipelines** — Fluffy treats each job as a single unit of work. For chained steps, conditional branching, or fan-out/fan-in patterns, consider Spring Batch or a workflow engine.
- **Cron / calendar-based scheduling** — Fluffy does not include a built-in scheduler. If you need recurring jobs on a cron expression, pair it with `@Scheduled`, Quartz, or an external scheduler.
- **Sub-second, latency-sensitive processing** — the REST + JPA overhead is minimal but non-trivial; for ultra-low-latency event processing, a reactive / streaming framework may be more appropriate.

## Quick Start

### Prerequisites

| Technology | Version |
|---|---|
| Java (JDK) | **21** or later (LTS) |
| Maven | **3.8** or later |
| Spring Boot | **3.4.x** (managed by the starter) |

### Build

```bash
mvn clean package
```

### Run Tests

```bash
mvn clean test
```

## Kubernetes Deployment

Fluffy is designed to run in Kubernetes. Use the Database or Kafka backend for multi-node deployments, enable recovery for fault tolerance, and leverage built-in metrics for autoscaling:

```yaml
fluffy:
  batch:
    backend:
      type: DATABASE
    recovery:
      enabled: true
    scaling:
      max-queue-depth: 100
```

See [Scaling Configuration](docs/scaling.md) for HPA and KEDA examples.

## Documentation

Detailed documentation is available in the [`docs/`](docs/) directory:

- [Architecture](docs/architecture.md)
- [Backend Configuration](docs/backends.md)
- [Job Definition](docs/job-definition.md)
- [Concurrency](docs/concurrency.md)
- [Data Source](docs/datasource.md)
- [REST API](docs/rest-api.md)
- [Retry & Stop](docs/retry-stop.md)
- [Dashboard](docs/dashboard.md)
- [Metrics](docs/metrics.md)
- [Scaling](docs/scaling.md)
- [Fault Tolerance & Recovery](docs/recovery.md)
- [Postman Collection](docs/fluffy-batch.postman_collection.json)

## Contributing

We welcome contributions! Please read our [Contributing Guidelines](CONTRIBUTING.md) before submitting a pull request.

## License

This project is licensed under the [Apache License 2.0](LICENSE).
