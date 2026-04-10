# Architecture

## Overview

Fluffy Batch Starter is a Spring Boot auto-configuration library that provides a batch job execution framework via REST APIs.

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
- **GlobalExceptionHandler**: Unified error handling.

### Auto-Configuration
- **BatchJobAutoConfiguration**: Registers all beans and sets up JPA.

## Flow

1. POST /api/jobs/{name}/start → JobController → JobLauncher.launch()
2. JobLauncher validates params, creates JobExecution (IN_QUEUE)
3. If concurrency allows: STARTED → IN_PROGRESS → SUCCESS/FAILURE
4. If concurrency full: stays IN_QUEUE in JobQueueManager
5. When a job finishes: processQueue() picks next pending job
