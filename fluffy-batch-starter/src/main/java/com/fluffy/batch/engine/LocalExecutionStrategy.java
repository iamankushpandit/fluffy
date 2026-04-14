package com.fluffy.batch.engine;

import com.fluffy.batch.api.JobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Default execution strategy that runs jobs in-process using virtual threads.
 *
 * <p>This preserves the original Fluffy Batch behaviour: synchronous or asynchronous
 * execution inside the JVM with configurable timeouts.</p>
 */
public class LocalExecutionStrategy implements ExecutionStrategy {

    private static final Logger log = LoggerFactory.getLogger(LocalExecutionStrategy.class);

    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;
    private final ExecutionCallback callback;

    private final Map<Long, JobContext> runningContexts = new ConcurrentHashMap<>();
    private final Map<Long, Future<?>> runningFutures = new ConcurrentHashMap<>();

    public LocalExecutionStrategy(
            @Qualifier("jobExecutorService") ExecutorService executorService,
            @Qualifier("jobScheduledExecutorService") ScheduledExecutorService scheduledExecutorService,
            ExecutionCallback callback) {
        this.executorService = executorService;
        this.scheduledExecutorService = scheduledExecutorService;
        this.callback = callback;
    }

    @Override
    public ExecutionMode getMode() {
        return ExecutionMode.LOCAL;
    }

    @Override
    public void execute(Long executionId, JobDefinition definition, JobContext context) {
        if (definition.async()) {
            runJobAsync(executionId, definition, context);
        } else {
            runJobSync(executionId, definition, context);
        }
    }

    @Override
    public boolean stop(Long executionId) {
        JobContext ctx = runningContexts.get(executionId);
        if (ctx != null) {
            ctx.markStopRequested();
            Future<?> future = runningFutures.get(executionId);
            if (future != null) {
                future.cancel(true);
            }
            return true;
        }
        return false;
    }

    /* package */ Map<Long, JobContext> getRunningContexts() {
        return runningContexts;
    }

    private void runJobAsync(Long executionId, JobDefinition def, JobContext context) {
        runningContexts.put(executionId, context);
        Future<?> future = executorService.submit(() -> executeJob(executionId, def, context));
        runningFutures.put(executionId, future);

        if (def.timeoutSeconds() > 0) {
            scheduledExecutorService.schedule(() -> {
                if (runningContexts.containsKey(executionId)) {
                    log.warn("Job {} timed out after {}s", executionId, def.timeoutSeconds());
                    context.markStopRequested();
                    Future<?> f = runningFutures.get(executionId);
                    if (f != null) f.cancel(true);
                }
            }, def.timeoutSeconds(), TimeUnit.SECONDS);
        }
    }

    private void runJobSync(Long executionId, JobDefinition def, JobContext context) {
        runningContexts.put(executionId, context);
        executeJob(executionId, def, context);
    }

    private void executeJob(Long executionId, JobDefinition def, JobContext context) {
        try {
            callback.onStarted(executionId);
            def.handler().execute(context);
            callback.onCompleted(executionId, def.name());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            callback.onStopped(executionId, def.name());
        } catch (Exception e) {
            callback.onFailed(executionId, def.name(), e.getMessage());
        } finally {
            runningContexts.remove(executionId);
            runningFutures.remove(executionId);
            callback.onFinished(executionId, def.name(), def.maxConcurrency());
        }
    }
}
