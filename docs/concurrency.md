# Concurrency

## Per-Job Concurrency

Each job definition has a `maxConcurrency` setting (default: 1).

When `maxConcurrency = 1`: only one instance runs at a time; extras are queued.
When `maxConcurrency = N`: up to N instances run concurrently.

## Queue Behavior

Jobs that cannot run immediately are placed in a FIFO queue (IN_QUEUE status).
When a running job finishes, the next queued job starts automatically.

## Async vs Sync

- `async = true`: runs on a Java 21 virtual thread via `Executors.newVirtualThreadPerTaskExecutor()`
- `async = false`: runs on the caller's thread (blocks the HTTP request)

Virtual threads are lightweight and ideal for I/O-bound jobs. Thousands of async jobs can run concurrently without exhausting OS threads.
