package com.fluffy.batch.backend;

import com.fluffy.batch.model.QueueEntry;
import com.fluffy.batch.persistence.QueueEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = com.fluffy.batch.TestBatchApplication.class)
@Transactional
class DbQueueBackendTest {

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    private DbQueueBackend queueBackend;

    @BeforeEach
    void setUp() {
        queueEntryRepository.deleteAll();
        queueBackend = new DbQueueBackend(queueEntryRepository, "test-node");
    }

    @Test
    void shouldEnqueueAndPoll() {
        queueBackend.enqueue(1L);
        queueBackend.enqueue(2L);

        assertThat(queueBackend.size()).isEqualTo(2);
        assertThat(queueBackend.poll()).isEqualTo(1L);
        assertThat(queueBackend.size()).isEqualTo(1);
        assertThat(queueBackend.poll()).isEqualTo(2L);
        assertThat(queueBackend.size()).isZero();
    }

    @Test
    void shouldReturnNullWhenPollingEmptyQueue() {
        assertThat(queueBackend.poll()).isNull();
    }

    @Test
    void shouldRemoveFromQueue() {
        queueBackend.enqueue(1L);
        queueBackend.enqueue(2L);

        boolean removed = queueBackend.remove(1L);
        assertThat(removed).isTrue();
        assertThat(queueBackend.size()).isEqualTo(1);
        assertThat(queueBackend.contains(1L)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenRemovingNonExistent() {
        assertThat(queueBackend.remove(999L)).isFalse();
    }

    @Test
    void shouldCheckContains() {
        queueBackend.enqueue(5L);

        assertThat(queueBackend.contains(5L)).isTrue();
        assertThat(queueBackend.contains(99L)).isFalse();
    }

    @Test
    void shouldReturnPosition() {
        queueBackend.enqueue(10L);
        queueBackend.enqueue(20L);

        assertThat(queueBackend.getPosition(10L)).isNotNull();
        assertThat(queueBackend.getPosition(20L)).isNotNull();
    }

    @Test
    void shouldReturnNullPositionForUnknownId() {
        assertThat(queueBackend.getPosition(999L)).isNull();
    }

    @Test
    void shouldSetClaimedByAndClaimedAtOnPoll() {
        queueBackend.enqueue(42L);

        Long polled = queueBackend.poll();
        assertThat(polled).isEqualTo(42L);

        QueueEntry entry = queueEntryRepository.findByExecutionId(42L).orElseThrow();
        assertThat(entry.getStatus()).isEqualTo("CLAIMED");
        assertThat(entry.getClaimedBy()).isEqualTo("test-node");
        assertThat(entry.getClaimedAt()).isNotNull();
    }

    @Test
    void shouldNotClaimAlreadyClaimedEntry() {
        queueBackend.enqueue(100L);

        // First poll claims it
        Long first = queueBackend.poll();
        assertThat(first).isEqualTo(100L);

        // Second poll should find nothing
        Long second = queueBackend.poll();
        assertThat(second).isNull();
    }
}
