# Job Definition

## Using @BatchJob Annotation

```java
@BatchJob(
    name = "my-job",
    description = "My job description",
    async = true,
    maxConcurrency = 2,
    timeoutSeconds = 60,
    requiredParams = {"param1", "param2"}
)
public class MyJob implements JobHandler {
    @Override
    public void execute(JobContext context) throws Exception {
        String param1 = context.requireParam("param1");
        // ... job logic
    }
}
```

## Using JobDefinition Builder

```java
JobDefinition def = JobDefinition.builder("my-job")
    .description("My job")
    .async(true)
    .maxConcurrency(2)
    .timeoutSeconds(60)
    .requiredParams("param1")
    .handler(ctx -> { /* logic */ })
    .build();
jobRegistry.register(def);
```

## JobContext

- `getParam(key)` - get optional parameter
- `requireParam(key)` - get required parameter, throws if missing
- `checkInterrupted()` - throws InterruptedException if stop was requested
- `getExecutionId()`, `getJobName()`, `getRequestedBy()` - metadata
