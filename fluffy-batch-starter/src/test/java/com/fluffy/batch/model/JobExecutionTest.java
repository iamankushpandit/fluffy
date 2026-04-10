package com.fluffy.batch.model;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class JobExecutionTest {

    @Test
    void shouldSetAndGetAllFields() {
        JobExecution execution = new JobExecution();
        Instant start = Instant.now();
        Instant end = start.plusSeconds(10);

        execution.setId(1L);
        execution.setJobName("test-job");
        execution.setStatus(BatchStatus.STARTED);
        execution.setRequestedBy("user1");
        execution.setStartTime(start);
        execution.setEndTime(end);
        execution.setParameters("{\"key\":\"value\"}");
        execution.setArguments("some-args");
        execution.setQueuePosition(3);
        execution.setErrorMessage("some error");

        assertThat(execution.getId()).isEqualTo(1L);
        assertThat(execution.getJobName()).isEqualTo("test-job");
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.STARTED);
        assertThat(execution.getRequestedBy()).isEqualTo("user1");
        assertThat(execution.getStartTime()).isEqualTo(start);
        assertThat(execution.getEndTime()).isEqualTo(end);
        assertThat(execution.getParameters()).isEqualTo("{\"key\":\"value\"}");
        assertThat(execution.getArguments()).isEqualTo("some-args");
        assertThat(execution.getQueuePosition()).isEqualTo(3);
        assertThat(execution.getErrorMessage()).isEqualTo("some error");
    }

    @Test
    void shouldHandleNullValues() {
        JobExecution execution = new JobExecution();

        assertThat(execution.getId()).isNull();
        assertThat(execution.getJobName()).isNull();
        assertThat(execution.getStatus()).isNull();
        assertThat(execution.getRequestedBy()).isNull();
        assertThat(execution.getStartTime()).isNull();
        assertThat(execution.getEndTime()).isNull();
        assertThat(execution.getParameters()).isNull();
        assertThat(execution.getArguments()).isNull();
        assertThat(execution.getQueuePosition()).isNull();
        assertThat(execution.getErrorMessage()).isNull();
    }
}
