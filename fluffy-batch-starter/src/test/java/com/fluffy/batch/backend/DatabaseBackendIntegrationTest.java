package com.fluffy.batch.backend;

import com.fluffy.batch.autoconfigure.BackendProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.*;

/**
 * Validates that the Database backend mode correctly creates DB-backed queue and coordination beans.
 */
@SpringBootTest(
        classes = com.fluffy.batch.TestBatchApplication.class,
        properties = "fluffy.batch.backend.type=database"
)
class DatabaseBackendIntegrationTest {

    @Autowired
    private QueueBackend queueBackend;

    @Autowired
    private CoordinationBackend coordinationBackend;

    @Autowired
    private BackendProperties backendProperties;

    @Test
    void shouldUseDatabaseBackendType() {
        assertThat(backendProperties.getType()).isEqualTo(BackendType.DATABASE);
    }

    @Test
    void shouldUseDbQueueBackend() {
        assertThat(queueBackend).isInstanceOf(DbQueueBackend.class);
    }

    @Test
    void shouldUseDbCoordinationBackend() {
        assertThat(coordinationBackend).isInstanceOf(DbCoordinationBackend.class);
    }

    @Test
    void shouldEnqueueAndPollWithDbQueue() {
        queueBackend.enqueue(2000L);
        assertThat(queueBackend.contains(2000L)).isTrue();
        assertThat(queueBackend.size()).isGreaterThanOrEqualTo(1);
        Long polled = queueBackend.poll();
        assertThat(polled).isEqualTo(2000L);
    }

    @Test
    void shouldTrackConcurrencyFromDatabase() {
        // DB coordination backend derives counts from execution status
        assertThat(coordinationBackend.canRun("db-test-job", 10)).isTrue();
        assertThat(coordinationBackend.getRunningCount("db-test-job")).isGreaterThanOrEqualTo(0);
    }
}
