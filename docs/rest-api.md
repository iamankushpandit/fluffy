# REST API

## Endpoints

### Start a Job
`POST /api/jobs/{jobName}/start`

Headers: `X-User-Id: username` (optional, defaults to "anonymous")

Body (optional):
```json
{
  "parameters": {"key": "value"},
  "arguments": "optional args",
  "requestedBy": "user"
}
```

### Get Job Status
`GET /api/jobs/{executionId}/status`

### Stop a Job
`POST /api/jobs/{executionId}/stop`

### Retry a Job
`POST /api/jobs/{executionId}/retry`

### List All Executions
`GET /api/jobs/executions`

### List Registered Jobs
`GET /api/jobs/registered`

## Response Format

```json
{
  "jobId": 1,
  "jobName": "my-job",
  "status": "SUCCESS",
  "requestedBy": "user",
  "startTime": "...",
  "endTime": "...",
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

- `IN_QUEUE` - waiting to run
- `STARTED` - picked up by executor
- `IN_PROGRESS` - handler.execute() running
- `SUCCESS` - completed successfully
- `FAILURE` - completed with error
- `STOPPED` - stopped by request
