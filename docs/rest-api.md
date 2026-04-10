# REST API Reference

## Job Endpoints

### List Registered Jobs

```
GET /api/jobs/registered
```

Returns all registered job definitions with their metadata.

Response:

```json
[
  {
    "name": "longRunningJob",
    "description": "A long-running example job",
    "maxConcurrency": 1,
    "async": true,
    "timeoutSeconds": 0,
    "requiredParams": []
  }
]
```

### Start a Job

```
POST /api/jobs/{jobName}/start
```

Headers: `X-User-Id: username` (optional, defaults to `"anonymous"`)

Body (optional):

```json
{
  "parameters": { "delay": "5" },
  "arguments": "optional args",
  "requestedBy": "user"
}
```

Response (`201 Created`):

```json
{
  "jobId": 1,
  "jobName": "longRunningJob",
  "status": "IN_QUEUE",
  "requestedBy": "user",
  "startTime": "2025-01-15T10:30:00Z",
  "endTime": null,
  "queuePosition": 0,
  "errorMessage": null
}
```

### Get Job Status

```
GET /api/jobs/{executionId}/status
```

Response:

```json
{
  "jobId": 1,
  "jobName": "longRunningJob",
  "status": "IN_PROGRESS",
  "requestedBy": "user",
  "startTime": "2025-01-15T10:30:00Z",
  "endTime": null,
  "queuePosition": null,
  "errorMessage": null
}
```

### Stop a Job

```
POST /api/jobs/{executionId}/stop
```

Removes the job from the queue or sets the stop flag on a running job. Jobs should call `context.checkInterrupted()` for graceful shutdown.

Response: same format as Get Job Status, with `status: "STOPPED"`.

### Retry a Job

```
POST /api/jobs/{executionId}/retry
```

Creates a new execution with the same parameters. Only works on finished jobs (`SUCCESS`, `FAILURE`, `STOPPED`).

Response: the new execution in the standard response format.

### List All Executions

```
GET /api/jobs/executions
```

Returns all job executions ordered by start time descending.

Response:

```json
[
  {
    "jobId": 2,
    "jobName": "longRunningJob",
    "status": "SUCCESS",
    "requestedBy": "user",
    "startTime": "2025-01-15T10:30:00Z",
    "endTime": "2025-01-15T10:30:05Z",
    "queuePosition": null,
    "errorMessage": null
  }
]
```

## Scaling Configuration Endpoints

### Get Scaling Config

```
GET /api/jobs/config/scaling
```

Response:

```json
{
  "maxQueueDepth": 100
}
```

### Update Scaling Config

```
POST /api/jobs/config/scaling
Content-Type: application/json
```

Body:

```json
{
  "maxQueueDepth": 200
}
```

Response:

```json
{
  "maxQueueDepth": 200
}
```

Requires `fluffy.batch.scaling.runtime-updates-enabled=true` (the default).

## Response Format

All job endpoints return a `JobStatusResponse`:

```json
{
  "jobId": 1,
  "jobName": "my-job",
  "status": "SUCCESS",
  "requestedBy": "user",
  "startTime": "2025-01-15T10:30:00Z",
  "endTime": "2025-01-15T10:30:05Z",
  "queuePosition": null,
  "errorMessage": null
}
```

## Error Format (RFC 7807)

Errors are returned as [RFC 7807 Problem Details](https://www.rfc-editor.org/rfc/rfc7807):

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Job not found: unknown-job"
}
```

## Status Values

| Status | Description |
|---|---|
| `IN_QUEUE` | Waiting to run |
| `STARTED` | Picked up by executor |
| `IN_PROGRESS` | `handler.execute()` running |
| `SUCCESS` | Completed successfully |
| `FAILURE` | Completed with error |
| `STOPPED` | Stopped by request |
