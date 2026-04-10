package com.fluffy.batch.backend;

import com.fluffy.batch.persistence.JobExecutionRepository;
import org.springframework.batch.core.BatchStatus;
import org.springframework.transaction.annotation.Transactional;

/**
 * Database-backed coordination implementation.
 * Determines running counts from the shared database so that multiple nodes
 * share a consistent view of concurrency state.
 */
public class DbCoordinationBackend implements CoordinationBackend {

    private final JobExecutionRepository executionRepository;

    public DbCoordinationBackend(JobExecutionRepository executionRepository) {
        this.executionRepository = executionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canRun(String jobName, int maxConcurrency) {
        return getRunningCount(jobName) < maxConcurrency;
    }

    @Override
    public void increment(String jobName) {
        // No-op: in database mode, the running count is derived from
        // execution status in the database, so there is nothing to increment.
    }

    @Override
    public void decrement(String jobName) {
        // No-op: in database mode, the running count is derived from
        // execution status in the database, so there is nothing to decrement.
    }

    @Override
    @Transactional(readOnly = true)
    public int getRunningCount(String jobName) {
        return (int) executionRepository.findByJobNameOrderByStartTimeDesc(jobName).stream()
                .filter(e -> e.getStatus() == BatchStatus.STARTED || e.getStatus() == BatchStatus.STARTING)
                .count();
    }

    @Override
    @Transactional(readOnly = true)
    public int getGlobalRunningCount() {
        return executionRepository.findByStatus(BatchStatus.STARTED).size()
                + executionRepository.findByStatus(BatchStatus.STARTING).size();
    }
}
