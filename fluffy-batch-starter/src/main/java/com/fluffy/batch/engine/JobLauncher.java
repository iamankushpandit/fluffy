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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JobLauncher {

    private static final Logger log = LoggerFactory.getLogger(JobLauncher.class);

    private final JobRegistry jobRegistry;
    private final CoordinationBackend coordinationBackend;
    private final JobExecutionRepository executionRepository;
    private final QueueBackend queueBackend;
    private final ObjectMapper objectMapper;
    private final Map<ExecutionMode, ExecutionStrategy> strategies;

    private final Map<Long, JobContext> queuedContexts = new ConcurrentHashMap<>();

    public JobLauncher(JobRegistry jobRegistry,
                       CoordinationBackend coordinationBackend,
                       JobExecutionRepository executionRepository,
                       QueueBackend queueBackend,
                       ObjectMapper objectMapper,
                       List<ExecutionStrategy> strategyList,
                       DefaultExecutionCallback executionCallback) {
        this.jobRegistry = jobRegistry;
        this.coordinationBackend = coordinationBackend;
        this.executionRepository = executionRepository;
        this.queueBackend = queueBackend;
        this.objectMapper = objectMapper;
        this.strategies = new ConcurrentHashMap<>();
        for (ExecutionStrategy s : strategyList) {
            strategies.put(s.getMode(), s);
        }
        // Wire the queue processor back to break the circular dependency
        executionCallback.setQueueProcessor(this::processQueue);
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
            ExecutionStrategy strategy = resolveStrategy(def.executionMode());
            strategy.execute(executionId, def, context);
        } else {
            queueBackend.enqueue(executionId);
            queuedContexts.put(executionId, context);
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
            JobContext ctx = queuedContexts.remove(executionId);
            if (ctx != null) ctx.markStopRequested();
            updateStatus(executionId, BatchStatus.STOPPED);
            return true;
        }

        // Try each strategy to stop the execution
        for (ExecutionStrategy strategy : strategies.values()) {
            if (strategy.stop(executionId)) {
                return true;
            }
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

    // ── Private helpers ────────────────────────────────────────────────

    private ExecutionStrategy resolveStrategy(ExecutionMode mode) {
        ExecutionStrategy strategy = strategies.get(mode);
        if (strategy == null) {
            log.warn("No strategy found for execution mode {}; falling back to LOCAL", mode);
            strategy = strategies.get(ExecutionMode.LOCAL);
        }
        if (strategy == null) {
            throw new IllegalStateException("No execution strategy available for mode: " + mode);
        }
        return strategy;
    }

    private void processQueue(String jobName, int maxConcurrency) {
        while (coordinationBackend.canRun(jobName, maxConcurrency)) {
            Long nextId = queueBackend.poll();
            if (nextId == null) {
                break;
            }
            JobContext nextContext = queuedContexts.get(nextId);
            if (nextContext == null) {
                log.warn("No context found for queued executionId={}, skipping", nextId);
                continue;
            }
            queuedContexts.remove(nextId);
            JobDefinition def = jobRegistry.get(jobName);
            coordinationBackend.increment(jobName);
            updateStatus(nextId, BatchStatus.STARTED);
            ExecutionStrategy strategy = resolveStrategy(def.executionMode());
            strategy.execute(nextId, def, nextContext);
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
