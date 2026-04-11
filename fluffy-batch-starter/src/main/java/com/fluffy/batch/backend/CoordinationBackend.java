package com.fluffy.batch.backend;

/**
 * Abstraction for job concurrency coordination.
 * Implementations may track running counts in-memory, in a shared database, or via a distributed system.
 */
public interface CoordinationBackend {

    /**
     * Returns {@code true} if the named job has capacity to run another instance.
     */
    boolean canRun(String jobName, int maxConcurrency);

    /**
     * Increments the running count for the named job.
     */
    void increment(String jobName);

    /**
     * Decrements the running count for the named job.
     */
    void decrement(String jobName);

    /**
     * Returns the current running count for the named job.
     */
    int getRunningCount(String jobName);

    /**
     * Returns the total running count across all jobs.
     */
    int getGlobalRunningCount();
}
