package com.fluffy.batch.backend;

import com.fluffy.batch.autoconfigure.BackendProperties;
import com.fluffy.batch.engine.ConcurrencyManager;
import com.fluffy.batch.persistence.JobQueueManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.*;

/**
 * Validates that the H2 backend mode correctly creates in-memory queue and coordination beans.
 */
@SpringBootTest(
        classes = com.fluffy.batch.TestBatchApplication.class,
        properties = "fluffy.batch.backend.type=h2"
)
class H2BackendIntegrationTest {

    @Autowired
    private QueueBackend queueBackend;

    @Autowired
    private CoordinationBackend coordinationBackend;

    @Autowired
    private BackendProperties backendProperties;

    @Test
    void shouldUseH2BackendType() {
        assertThat(backendProperties.getType()).isEqualTo(BackendType.H2);
    }

    @Test
    void shouldUseInMemoryQueueBackend() {
        assertThat(queueBackend).isInstanceOf(JobQueueManager.class);
    }

    @Test
    void shouldUseInMemoryCoordinationBackend() {
        assertThat(coordinationBackend).isInstanceOf(ConcurrencyManager.class);
    }

    @Test
    void shouldEnqueueAndPollWithH2Queue() {
        queueBackend.enqueue(1000L);
        assertThat(queueBackend.contains(1000L)).isTrue();
        assertThat(queueBackend.poll()).isEqualTo(1000L);
        assertThat(queueBackend.contains(1000L)).isFalse();
    }

    @Test
    void shouldTrackConcurrencyWithH2Coordination() {
        assertThat(coordinationBackend.canRun("h2-test-job", 2)).isTrue();
        coordinationBackend.increment("h2-test-job");
        assertThat(coordinationBackend.getRunningCount("h2-test-job")).isEqualTo(1);
        coordinationBackend.decrement("h2-test-job");
        assertThat(coordinationBackend.getRunningCount("h2-test-job")).isZero();
    }
}
