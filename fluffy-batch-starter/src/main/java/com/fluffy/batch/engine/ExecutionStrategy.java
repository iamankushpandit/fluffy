package com.fluffy.batch.engine;

import com.fluffy.batch.api.JobContext;

/**
 * Abstraction for job execution strategies.
 *
 * <p>Implementations decide <em>where</em> and <em>how</em> a job is executed.
 * The default {@link LocalExecutionStrategy} runs jobs in-process using virtual
 * threads, while {@link CloudNativeExecutionStrategy} delegates to an external
 * orchestrator such as Kubernetes CronJobs, Apache Airflow, AWS Batch, etc.</p>
 */
public interface ExecutionStrategy {

    /**
     * Returns the {@link ExecutionMode} this strategy handles.
     */
    ExecutionMode getMode();

    /**
     * Submits a job for execution.
     *
     * @param executionId unique identifier for this run
     * @param definition  the job definition (name, handler, timeout, etc.)
     * @param context     the runtime context (parameters, requestedBy, etc.)
     */
    void execute(Long executionId, JobDefinition definition, JobContext context);

    /**
     * Attempts to stop a running job.
     *
     * @param executionId unique identifier of the execution to stop
     * @return {@code true} if a stop signal was sent successfully
     */
    boolean stop(Long executionId);
}
