package com.fluffy.batch.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fluffy.batch.api.JobContext;
import com.fluffy.batch.api.JobRequest;
import com.fluffy.batch.backend.CoordinationBackend;
import com.fluffy.batch.backend.QueueBackend;
import com.fluffy.batch.model.JobExecution;
import com.fluffy.batch.persistence.JobExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class JobLauncher {

    private static final Logger log = LoggerFactory.getLogger(JobLauncher.class);

    private final JobRegistry jobRegistry;
    private final CoordinationBackend coordinationBackend;
    private final JobExecutionRepository executionRepository;
    private final QueueBackend queueBackend;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;
    private final ObjectMapper objectMapper;

    private final Map<Long, JobContext> runningContexts = new ConcurrentHashMap<>();
    private final Map<Long, Future<?>> runningFutures = new ConcurrentHashMap<>();

    public JobLauncher(JobRegistry jobRegistry,
                       CoordinationBackend coordinationBackend,
                       JobExecutionRepository executionRepository,
                       QueueBackend queueBackend,
                       @Qualifier("jobExecutorService") ExecutorService executorService,
                       @Qualifier("jobScheduledExecutorService") ScheduledExecutorService scheduledExecutorService,
                       ObjectMapper objectMapper) {
        this.jobRegistry = jobRegistry;
        this.coordinationBackend = coordinationBackend;
        this.executionRepository = executionRepository;
        this.queueBackend = queueBackend;
        this.executorService = executorService;
        this.scheduledExecutorService = scheduledExecutorService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Long launch(String jobName, JobRequest request) {
        if (request == null) {
            request = new JobRequest();
        }

        JobDefinition def = jobRegistry.get(jobName);

        Map<String, String> params = new HashMap<>(request.parameters());

        for (String required : def.requiredParams()) {
            if (!params.containsKey(required) || params.get(required) == null) {
                throw new IllegalArgumentException("Missing required parameter: " + required);
            }
        }

        String requestedBy = request.requestedBy() != null ? request.requestedBy() : "anonymous";

        JobExecution execution = new JobExecution();
        execution.setJobName(jobName);
        execution.setStatus(BatchStatus.STARTING);
        execution.setRequestedBy(requestedBy);
        execution.setStartTime(Instant.now());
        execution.setArguments(request.arguments());
        execution.setParameters(paramsToString(params));
        execution = executionRepository.save(execution);

        Long executionId = execution.getId();

        JobContext context = new JobContext(executionId, jobName, requestedBy, params, request.arguments());

        if (coordinationBackend.canRun(jobName, def.maxConcurrency())) {
            coordinationBackend.increment(jobName);
            updateStatus(executionId, BatchStatus.STARTED);
            if (def.async()) {
                runJobAsync(executionId, def, context);
            } else {
                runJobSync(executionId, def, context);
            }
        } else {
            queueBackend.enqueue(executionId);
            runningContexts.put(executionId, context);
            Integer pos = queueBackend.getPosition(executionId);
            JobExecution queued = executionRepository.findById(executionId).orElseThrow();
            queued.setQueuePosition(pos);
            executionRepository.save(queued);
            log.info("Job {} queued at position {} (executionId={})", jobName, pos, executionId);
        }

        return executionId;
    }

    public boolean stop(Long executionId) {
        if (queueBackend.contains(executionId)) {
            queueBackend.remove(executionId);
            JobContext ctx = runningContexts.remove(executionId);
            if (ctx != null) ctx.markStopRequested();
            updateStatus(executionId, BatchStatus.STOPPED);
            return true;
        }

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

    @Transactional
    public Long retry(Long executionId) {
        JobExecution original = executionRepository.findById(executionId)
                .orElseThrow(() -> new JobNotFoundException("Execution not found: " + executionId));

        BatchStatus currentStatus = original.getStatus();
        if (currentStatus == BatchStatus.STARTING || currentStatus == BatchStatus.STARTED) {
            throw new IllegalStateException("Cannot retry a job that is still running or queued");
        }

        JobRequest request = new JobRequest(
                stringToParams(original.getParameters()),
                original.getArguments(),
                original.getRequestedBy()
        );

        return launch(original.getJobName(), request);
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
            updateStatus(executionId, BatchStatus.STARTED);
            def.handler().execute(context);
            updateStatus(executionId, BatchStatus.COMPLETED);
            log.info("Job {} completed successfully (executionId={})", def.name(), executionId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            updateStatus(executionId, BatchStatus.STOPPED);
            log.info("Job {} was stopped (executionId={})", def.name(), executionId);
        } catch (Exception e) {
            log.error("Job {} failed (executionId={}): {}", def.name(), executionId, e.getMessage(), e);
            updateStatusWithError(executionId, BatchStatus.FAILED, e.getMessage());
        } finally {
            runningContexts.remove(executionId);
            runningFutures.remove(executionId);
            coordinationBackend.decrement(def.name());
            setEndTime(executionId);
            processQueue(def.name(), def.maxConcurrency());
        }
    }

    private void processQueue(String jobName, int maxConcurrency) {
        while (coordinationBackend.canRun(jobName, maxConcurrency)) {
            Long nextId = queueBackend.poll();
            if (nextId == null) {
                break;
            }
            JobContext nextContext = runningContexts.get(nextId);
            if (nextContext == null) {
                log.warn("No context found for queued executionId={}, skipping", nextId);
                continue;
            }
            JobDefinition def = jobRegistry.get(jobName);
            coordinationBackend.increment(jobName);
            updateStatus(nextId, BatchStatus.STARTED);
            if (def.async()) {
                runJobAsync(nextId, def, nextContext);
            } else {
                runJobSync(nextId, def, nextContext);
            }
            break;
        }
    }

    private void updateStatus(Long executionId, BatchStatus status) {
        executionRepository.findById(executionId).ifPresent(exec -> {
            exec.setStatus(status);
            if (status == BatchStatus.STARTED) {
                exec.setQueuePosition(null);
            }
            executionRepository.save(exec);
        });
    }

    private void updateStatusWithError(Long executionId, BatchStatus status, String errorMessage) {
        executionRepository.findById(executionId).ifPresent(exec -> {
            exec.setStatus(status);
            exec.setErrorMessage(errorMessage != null && errorMessage.length() > 4096
                    ? errorMessage.substring(0, 4096) : errorMessage);
            executionRepository.save(exec);
        });
    }

    private void setEndTime(Long executionId) {
        executionRepository.findById(executionId).ifPresent(exec -> {
            exec.setEndTime(Instant.now());
            executionRepository.save(exec);
        });
    }

    private String paramsToString(Map<String, String> params) {
        if (params == null || params.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            log.warn("Failed to serialize parameters", e);
            return null;
        }
    }

    private Map<String, String> stringToParams(String paramsStr) {
        if (paramsStr == null || paramsStr.isBlank()) return new HashMap<>();
        try {
            return objectMapper.readValue(paramsStr, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.warn("Failed to deserialize parameters: {}", paramsStr, e);
            return new HashMap<>();
        }
    }
}
