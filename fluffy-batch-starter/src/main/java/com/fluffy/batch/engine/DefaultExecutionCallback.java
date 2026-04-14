package com.fluffy.batch.engine;

import com.fluffy.batch.backend.CoordinationBackend;
import com.fluffy.batch.model.JobExecution;
import com.fluffy.batch.persistence.JobExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;

import java.time.Instant;

/**
 * Default implementation of {@link ExecutionCallback} that persists
 * job status changes and coordinates queue processing.
 */
public class DefaultExecutionCallback implements ExecutionCallback {

    private static final Logger log = LoggerFactory.getLogger(DefaultExecutionCallback.class);

    private final JobExecutionRepository executionRepository;
    private final CoordinationBackend coordinationBackend;
    private volatile QueueProcessor queueProcessor;

    public DefaultExecutionCallback(JobExecutionRepository executionRepository,
                                    CoordinationBackend coordinationBackend) {
        this.executionRepository = executionRepository;
        this.coordinationBackend = coordinationBackend;
    }

    /**
     * Set lazily to break the circular dependency between callback → launcher → strategy → callback.
     */
    public void setQueueProcessor(QueueProcessor queueProcessor) {
        this.queueProcessor = queueProcessor;
    }

    @Override
    public void onStarted(Long executionId) {
        updateStatus(executionId, BatchStatus.STARTED);
    }

    @Override
    public void onCompleted(Long executionId, String jobName) {
        updateStatus(executionId, BatchStatus.COMPLETED);
        log.info("Job {} completed successfully (executionId={})", jobName, executionId);
    }

    @Override
    public void onStopped(Long executionId, String jobName) {
        updateStatus(executionId, BatchStatus.STOPPED);
        log.info("Job {} was stopped (executionId={})", jobName, executionId);
    }

    @Override
    public void onFailed(Long executionId, String jobName, String errorMessage) {
        log.error("Job {} failed (executionId={}): {}", jobName, executionId, errorMessage);
        updateStatusWithError(executionId, BatchStatus.FAILED, errorMessage);
    }

    @Override
    public void onFinished(Long executionId, String jobName, int maxConcurrency) {
        coordinationBackend.decrement(jobName);
        setEndTime(executionId);
        if (queueProcessor != null) {
            queueProcessor.processQueue(jobName, maxConcurrency);
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

    /**
     * Callback interface so the launcher can register its queue-processing logic
     * without creating a circular dependency.
     */
    @FunctionalInterface
    public interface QueueProcessor {
        void processQueue(String jobName, int maxConcurrency);
    }
}
