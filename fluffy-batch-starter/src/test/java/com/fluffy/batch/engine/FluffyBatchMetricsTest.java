package com.fluffy.batch.engine;

import com.fluffy.batch.backend.CoordinationBackend;
import com.fluffy.batch.backend.QueueBackend;
import com.fluffy.batch.persistence.JobExecutionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FluffyBatchMetricsTest {

    @Mock
    private QueueBackend queueBackend;

    @Mock
    private CoordinationBackend coordinationBackend;

    @Mock
    private JobExecutionRepository executionRepository;

    private MeterRegistry registry;
    private FluffyBatchMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        when(executionRepository.findByStatus(BatchStatus.COMPLETED)).thenReturn(Collections.emptyList());
        when(executionRepository.findByStatus(BatchStatus.FAILED)).thenReturn(Collections.emptyList());
        when(executionRepository.findByStatus(BatchStatus.STOPPED)).thenReturn(Collections.emptyList());
        metrics = new FluffyBatchMetrics(registry, queueBackend, coordinationBackend, executionRepository);
    }

    @Test
    void shouldRegisterQueueDepthGauge() {
        assertThat(registry.find("fluffy.batch.queue.depth").gauge()).isNotNull();
    }

    @Test
    void shouldRegisterActiveJobsGauge() {
        assertThat(registry.find("fluffy.batch.jobs.active").gauge()).isNotNull();
    }

    @Test
    void shouldRegisterCompletedCounter() {
        assertThat(registry.find("fluffy.batch.jobs.completed.total").counter()).isNotNull();
    }

    @Test
    void shouldRegisterFailedCounter() {
        assertThat(registry.find("fluffy.batch.jobs.failed.total").counter()).isNotNull();
    }

    @Test
    void shouldRegisterStoppedCounter() {
        assertThat(registry.find("fluffy.batch.jobs.stopped.total").counter()).isNotNull();
    }

    @Test
    void shouldIncrementCompletedCounter() {
        double before = registry.find("fluffy.batch.jobs.completed.total").counter().count();
        metrics.recordCompleted();
        double after = registry.find("fluffy.batch.jobs.completed.total").counter().count();
        assertThat(after).isGreaterThan(before);
    }

    @Test
    void shouldIncrementFailedCounter() {
        double before = registry.find("fluffy.batch.jobs.failed.total").counter().count();
        metrics.recordFailed();
        double after = registry.find("fluffy.batch.jobs.failed.total").counter().count();
        assertThat(after).isGreaterThan(before);
    }

    @Test
    void shouldIncrementStoppedCounter() {
        double before = registry.find("fluffy.batch.jobs.stopped.total").counter().count();
        metrics.recordStopped();
        double after = registry.find("fluffy.batch.jobs.stopped.total").counter().count();
        assertThat(after).isGreaterThan(before);
    }
}
