package com.fluffy.batch.engine;

/**
 * Callback interface used by {@link ExecutionStrategy} implementations to report
 * lifecycle events back to the {@link JobLauncher}.
 *
 * <p>This decouples strategy implementations from persistence and coordination
 * concerns.</p>
 */
public interface ExecutionCallback {

    /** Called when a job has actually started executing. */
    void onStarted(Long executionId);

    /** Called when a job completes successfully. */
    void onCompleted(Long executionId, String jobName);

    /** Called when a job is stopped / interrupted. */
    void onStopped(Long executionId, String jobName);

    /** Called when a job fails. */
    void onFailed(Long executionId, String jobName, String errorMessage);

    /**
     * Called after execution finishes (success, failure, or stop).
     * Implementations should release coordination resources and process the queue.
     */
    void onFinished(Long executionId, String jobName, int maxConcurrency);
}
