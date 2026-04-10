package com.fluffy.batch.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class JobStatusResponseTest {

    @Test
    void shouldCreateFromJobExecution() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(10);

        JobExecution execution = new JobExecution();
        execution.setId(1L);
        execution.setJobName("test-job");
        execution.setStatus(JobStatus.SUCCESS);
        execution.setRequestedBy("user1");
        execution.setStartTime(start);
        execution.setEndTime(end);
        execution.setQueuePosition(null);
        execution.setErrorMessage(null);

        JobStatusResponse response = JobStatusResponse.from(execution);

        assertThat(response.jobId()).isEqualTo(1L);
        assertThat(response.jobName()).isEqualTo("test-job");
        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.requestedBy()).isEqualTo("user1");
        assertThat(response.startTime()).isEqualTo(start);
        assertThat(response.endTime()).isEqualTo(end);
        assertThat(response.queuePosition()).isNull();
        assertThat(response.errorMessage()).isNull();
    }

    @Test
    void shouldHandleNullStatus() {
        JobExecution execution = new JobExecution();
        execution.setId(2L);
        execution.setJobName("test-job");
        execution.setStatus(null);

        JobStatusResponse response = JobStatusResponse.from(execution);

        assertThat(response.status()).isNull();
    }

    @Test
    void shouldHandleQueuePositionAndError() {
        JobExecution execution = new JobExecution();
        execution.setId(3L);
        execution.setJobName("test-job");
        execution.setStatus(JobStatus.FAILURE);
        execution.setQueuePosition(5);
        execution.setErrorMessage("Something went wrong");

        JobStatusResponse response = JobStatusResponse.from(execution);

        assertThat(response.queuePosition()).isEqualTo(5);
        assertThat(response.errorMessage()).isEqualTo("Something went wrong");
    }
}
