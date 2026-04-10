# Retry and Stop

## Stopping a Job

`POST /api/jobs/{executionId}/stop`

- If queued: removes from queue, marks STOPPED
- If running: sets stopRequested flag and interrupts the thread

Jobs should call `context.checkInterrupted()` periodically to support graceful stop.

## Retrying a Job

`POST /api/jobs/{executionId}/retry`

Creates a new execution with the same parameters as the original.
Only works on finished jobs (SUCCESS, FAILURE, STOPPED).
