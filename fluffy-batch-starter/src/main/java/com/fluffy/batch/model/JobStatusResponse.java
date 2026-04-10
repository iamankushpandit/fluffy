package com.fluffy.batch.model;

import java.time.Instant;

/**
 * Immutable response DTO representing the current state of a job execution.
 */
public record JobStatusResponse(
        Long jobId,
        String jobName,
        String status,
        String requestedBy,
        Instant startTime,
        Instant endTime,
        Integer queuePosition,
        String errorMessage
) {
    /** Creates a response from a {@link JobExecution} entity. */
    public static JobStatusResponse from(JobExecution execution) {
        return new JobStatusResponse(
                execution.getId(),
                execution.getJobName(),
                execution.getStatus() != null ? execution.getStatus().name() : null,
                execution.getRequestedBy(),
                execution.getStartTime(),
                execution.getEndTime(),
                execution.getQueuePosition(),
                execution.getErrorMessage()
        );
    }
}
