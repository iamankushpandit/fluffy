package com.fluffy.batch.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JobStatusTest {

    @Test
    void shouldHaveAllExpectedValues() {
        assertThat(JobStatus.values()).containsExactly(
                JobStatus.IN_QUEUE,
                JobStatus.STARTED,
                JobStatus.IN_PROGRESS,
                JobStatus.SUCCESS,
                JobStatus.FAILURE,
                JobStatus.STOPPED
        );
    }

    @Test
    void shouldResolveFromName() {
        assertThat(JobStatus.valueOf("IN_QUEUE")).isEqualTo(JobStatus.IN_QUEUE);
        assertThat(JobStatus.valueOf("STARTED")).isEqualTo(JobStatus.STARTED);
        assertThat(JobStatus.valueOf("IN_PROGRESS")).isEqualTo(JobStatus.IN_PROGRESS);
        assertThat(JobStatus.valueOf("SUCCESS")).isEqualTo(JobStatus.SUCCESS);
        assertThat(JobStatus.valueOf("FAILURE")).isEqualTo(JobStatus.FAILURE);
        assertThat(JobStatus.valueOf("STOPPED")).isEqualTo(JobStatus.STOPPED);
    }
}
