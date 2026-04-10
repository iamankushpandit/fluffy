package com.fluffy.batch.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JobQueueManagerTest {

    private JobQueueManager queueManager;

    @BeforeEach
    void setUp() {
        queueManager = new JobQueueManager();
    }

    @Test
    void shouldEnqueueAndPoll() {
        queueManager.enqueue(1L);
        queueManager.enqueue(2L);

        assertThat(queueManager.size()).isEqualTo(2);
        assertThat(queueManager.poll()).isEqualTo(1L);
        assertThat(queueManager.poll()).isEqualTo(2L);
        assertThat(queueManager.size()).isZero();
    }

    @Test
    void shouldReturnNullWhenPollingEmptyQueue() {
        assertThat(queueManager.poll()).isNull();
    }

    @Test
    void shouldAssignPositions() {
        queueManager.enqueue(10L);
        queueManager.enqueue(20L);

        assertThat(queueManager.getPosition(10L)).isEqualTo(1);
        assertThat(queueManager.getPosition(20L)).isEqualTo(2);
    }

    @Test
    void shouldReturnNullPositionForUnknownId() {
        assertThat(queueManager.getPosition(999L)).isNull();
    }

    @Test
    void shouldRemoveFromQueue() {
        queueManager.enqueue(1L);
        queueManager.enqueue(2L);

        boolean removed = queueManager.remove(1L);
        assertThat(removed).isTrue();
        assertThat(queueManager.size()).isEqualTo(1);
        assertThat(queueManager.contains(1L)).isFalse();
        assertThat(queueManager.getPosition(1L)).isNull();
    }

    @Test
    void shouldReturnFalseWhenRemovingNonExistent() {
        assertThat(queueManager.remove(999L)).isFalse();
    }

    @Test
    void shouldCheckContains() {
        queueManager.enqueue(5L);

        assertThat(queueManager.contains(5L)).isTrue();
        assertThat(queueManager.contains(99L)).isFalse();
    }

    @Test
    void shouldRemovePositionOnPoll() {
        queueManager.enqueue(1L);
        assertThat(queueManager.getPosition(1L)).isNotNull();

        queueManager.poll();
        assertThat(queueManager.getPosition(1L)).isNull();
    }

    @Test
    void shouldReportCorrectSize() {
        assertThat(queueManager.size()).isZero();
        queueManager.enqueue(1L);
        assertThat(queueManager.size()).isEqualTo(1);
        queueManager.enqueue(2L);
        assertThat(queueManager.size()).isEqualTo(2);
        queueManager.poll();
        assertThat(queueManager.size()).isEqualTo(1);
    }
}
