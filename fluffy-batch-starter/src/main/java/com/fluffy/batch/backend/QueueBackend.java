package com.fluffy.batch.backend;

/**
 * Abstraction for job queue operations.
 * Implementations may store queue state in-memory, in a database, or in a message broker.
 */
public interface QueueBackend {

    /**
     * Adds an execution to the queue.
     *
     * @param executionId the execution identifier
     */
    void enqueue(Long executionId);

    /**
     * Removes and returns the next queued execution identifier, or {@code null} if the queue is empty.
     */
    Long poll();

    /**
     * Removes a specific execution from the queue.
     *
     * @return {@code true} if the execution was present and removed
     */
    boolean remove(Long executionId);

    /**
     * Returns the queue position of the given execution, or {@code null} if not queued.
     */
    Integer getPosition(Long executionId);

    /**
     * Returns the current number of entries in the queue.
     */
    int size();

    /**
     * Returns {@code true} if the execution is currently queued.
     */
    boolean contains(Long executionId);
}
