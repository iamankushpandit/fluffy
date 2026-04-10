package com.fluffy.batch.engine;

import com.fluffy.batch.api.JobContext;
import com.fluffy.batch.api.JobRequest;
import com.fluffy.batch.model.JobExecution;
import com.fluffy.batch.model.JobStatus;
import com.fluffy.batch.persistence.JobExecutionRepository;
import com.fluffy.batch.persistence.JobQueueManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
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
    private final ConcurrencyManager concurrencyManager;
    private final JobExecutionRepository executionRepository;
    private final JobQueueManager queueManager;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;

    private final Map<Long, JobContext> runningContexts = new ConcurrentHashMap<>();
    private final Map<Long, Future<?>> runningFutures = new ConcurrentHashMap<>();

    public JobLauncher(JobRegistry jobRegistry,
                       ConcurrencyManager concurrencyManager,
                       JobExecutionRepository executionRepository,
                       JobQueueManager queueManager,
                       @Qualifier("jobExecutorService") ExecutorService executorService,
                       @Qualifier("jobScheduledExecutorService") ScheduledExecutorService scheduledExecutorService) {
        this.jobRegistry = jobRegistry;
        this.concurrencyManager = concurrencyManager;
        this.executionRepository = executionRepository;
        this.queueManager = queueManager;
        this.executorService = executorService;
        this.scheduledExecutorService = scheduledExecutorService;
    }

    @Transactional
    public Long launch(String jobName, JobRequest request) {
        if (request == null) {
            request = new JobRequest();
        }

        JobDefinition def = jobRegistry.get(jobName);

        Map<String, String> params = request.getParameters() != null
                ? new HashMap<>(request.getParameters())
                : new HashMap<>();

        for (String required : def.getRequiredParams()) {
            if (!params.containsKey(required) || params.get(required) == null) {
                throw new IllegalArgumentException("Missing required parameter: " + required);
            }
        }

        String requestedBy = request.getRequestedBy() != null ? request.getRequestedBy() : "anonymous";

        JobExecution execution = new JobExecution();
        execution.setJobName(jobName);
        execution.setStatus(JobStatus.IN_QUEUE);
        execution.setRequestedBy(requestedBy);
        execution.setStartTime(Timestamp.from(Instant.now()));
        execution.setArguments(request.getArguments());
        execution.setParameters(paramsToString(params));
        execution = executionRepository.save(execution);

        Long executionId = execution.getId();

        JobContext context = new JobContext(executionId, jobName, requestedBy, params, request.getArguments());

        if (concurrencyManager.canRun(jobName, def.getMaxConcurrency())) {
            concurrencyManager.increment(jobName);
            updateStatus(executionId, JobStatus.STARTED);
            if (def.isAsync()) {
                runJobAsync(executionId, def, context);
            } else {
                runJobSync(executionId, def, context);
            }
        } else {
            queueManager.enqueue(executionId);
            runningContexts.put(executionId, context);
            Integer pos = queueManager.getPosition(executionId);
            JobExecution queued = executionRepository.findById(executionId).orElseThrow();
            queued.setQueuePosition(pos);
            executionRepository.save(queued);
            log.info("Job {} queued at position {} (executionId={})", jobName, pos, executionId);
        }

        return executionId;
    }

    public boolean stop(Long executionId) {
        if (queueManager.contains(executionId)) {
            queueManager.remove(executionId);
            JobContext ctx = runningContexts.remove(executionId);
            if (ctx != null) ctx.markStopRequested();
            updateStatus(executionId, JobStatus.STOPPED);
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

        JobStatus currentStatus = original.getStatus();
        if (currentStatus == JobStatus.IN_QUEUE || currentStatus == JobStatus.STARTED
                || currentStatus == JobStatus.IN_PROGRESS) {
            throw new IllegalStateException("Cannot retry a job that is still running or queued");
        }

        JobRequest request = new JobRequest();
        request.setRequestedBy(original.getRequestedBy());
        request.setArguments(original.getArguments());
        request.setParameters(stringToParams(original.getParameters()));

        return launch(original.getJobName(), request);
    }

    private void runJobAsync(Long executionId, JobDefinition def, JobContext context) {
        runningContexts.put(executionId, context);
        Future<?> future = executorService.submit(() -> executeJob(executionId, def, context));
        runningFutures.put(executionId, future);

        if (def.getTimeoutSeconds() > 0) {
            scheduledExecutorService.schedule(() -> {
                if (runningContexts.containsKey(executionId)) {
                    log.warn("Job {} timed out after {}s", executionId, def.getTimeoutSeconds());
                    context.markStopRequested();
                    Future<?> f = runningFutures.get(executionId);
                    if (f != null) f.cancel(true);
                }
            }, def.getTimeoutSeconds(), TimeUnit.SECONDS);
        }
    }

    private void runJobSync(Long executionId, JobDefinition def, JobContext context) {
        runningContexts.put(executionId, context);
        executeJob(executionId, def, context);
    }

    private void executeJob(Long executionId, JobDefinition def, JobContext context) {
        try {
            updateStatus(executionId, JobStatus.IN_PROGRESS);
            def.getHandler().execute(context);
            updateStatus(executionId, JobStatus.SUCCESS);
            log.info("Job {} completed successfully (executionId={})", def.getName(), executionId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            updateStatus(executionId, JobStatus.STOPPED);
            log.info("Job {} was stopped (executionId={})", def.getName(), executionId);
        } catch (Exception e) {
            log.error("Job {} failed (executionId={}): {}", def.getName(), executionId, e.getMessage(), e);
            updateStatusWithError(executionId, JobStatus.FAILURE, e.getMessage());
        } finally {
            runningContexts.remove(executionId);
            runningFutures.remove(executionId);
            concurrencyManager.decrement(def.getName());
            setEndTime(executionId);
            processQueue(def.getName(), def.getMaxConcurrency());
        }
    }

    private void processQueue(String jobName, int maxConcurrency) {
        if (concurrencyManager.canRun(jobName, maxConcurrency)) {
            Long nextId = queueManager.poll();
            if (nextId != null) {
                JobContext nextContext = runningContexts.get(nextId);
                if (nextContext == null) {
                    processQueue(jobName, maxConcurrency);
                    return;
                }
                JobDefinition def = jobRegistry.get(jobName);
                concurrencyManager.increment(jobName);
                updateStatus(nextId, JobStatus.STARTED);
                if (def.isAsync()) {
                    runJobAsync(nextId, def, nextContext);
                } else {
                    runJobSync(nextId, def, nextContext);
                }
            }
        }
    }

    private void updateStatus(Long executionId, JobStatus status) {
        executionRepository.findById(executionId).ifPresent(exec -> {
            exec.setStatus(status);
            if (status == JobStatus.STARTED || status == JobStatus.IN_PROGRESS) {
                exec.setQueuePosition(null);
            }
            executionRepository.save(exec);
        });
    }

    private void updateStatusWithError(Long executionId, JobStatus status, String errorMessage) {
        executionRepository.findById(executionId).ifPresent(exec -> {
            exec.setStatus(status);
            exec.setErrorMessage(errorMessage != null && errorMessage.length() > 4000
                    ? errorMessage.substring(0, 4000) : errorMessage);
            executionRepository.save(exec);
        });
    }

    private void setEndTime(Long executionId) {
        executionRepository.findById(executionId).ifPresent(exec -> {
            exec.setEndTime(Timestamp.from(Instant.now()));
            executionRepository.save(exec);
        });
    }

    private String paramsToString(Map<String, String> params) {
        if (params == null || params.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("{");
        params.forEach((k, v) -> sb.append("\"").append(k).append("\":\"").append(v).append("\","));
        if (sb.charAt(sb.length() - 1) == ',') sb.deleteCharAt(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }

    private Map<String, String> stringToParams(String paramsStr) {
        Map<String, String> params = new HashMap<>();
        if (paramsStr == null || paramsStr.isBlank()) return params;
        String content = paramsStr.trim();
        if (content.startsWith("{")) content = content.substring(1);
        if (content.endsWith("}")) content = content.substring(0, content.length() - 1);
        String[] pairs = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replaceAll("^\"|\"$", "");
                String value = kv[1].trim().replaceAll("^\"|\"$", "");
                params.put(key, value);
            }
        }
        return params;
    }
}
