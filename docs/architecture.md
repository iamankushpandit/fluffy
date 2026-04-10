# Architecture

## Overview

Fluffy Batch Starter is a Spring Boot auto-configuration library that provides a batch job execution framework via REST APIs. It targets **Java 21+** and **Spring Boot 3.4+**, leveraging modern language features and runtime capabilities.

## Java 21 & Spring Boot 3.4 Features

- **Records** — `JobRequest`, `JobStatusResponse`, and `JobDefinition` are immutable Java records, reducing boilerplate and improving thread safety.
- **Virtual Threads** — The job executor uses `Executors.newVirtualThreadPerTaskExecutor()`, enabling lightweight, high-throughput concurrency ideal for I/O-bound batch jobs.
- **`java.time.Instant`** — All timestamps use `Instant` instead of the legacy `java.sql.Timestamp`.
- **RFC 7807 Problem Details** — Error responses follow the `ProblemDetail` standard for consistent, machine-readable error payloads.

## Components

### Core Engine
- **JobRegistry**: Stores job definitions. Auto-discovers `@BatchJob` beans.
- **JobLauncher**: Orchestrates job lifecycle (launch, stop, retry).
- **ConcurrencyManager**: Tracks running job counts per job name.
- **JobQueueManager**: In-memory FIFO queue for pending jobs.

### Persistence
- **JobExecution**: JPA entity storing execution state.
- **JobExecutionRepository**: Spring Data JPA repository.

### Web Layer
- **JobController**: REST endpoints for job management.
- **GlobalExceptionHandler**: Unified error handling using `ProblemDetail`.

### Auto-Configuration
- **BatchJobAutoConfiguration**: Registers all beans, sets up JPA, and configures virtual-thread-based executors.

## Flow

1. POST /api/jobs/{name}/start → JobController → JobLauncher.launch()
2. JobLauncher validates params, creates JobExecution (IN_QUEUE)
3. If concurrency allows: STARTED → IN_PROGRESS → SUCCESS/FAILURE
4. If concurrency full: stays IN_QUEUE in JobQueueManager
5. When a job finishes: processQueue() picks next pending job
