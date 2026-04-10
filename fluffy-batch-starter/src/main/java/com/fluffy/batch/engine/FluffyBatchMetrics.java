package com.fluffy.batch.engine;

import com.fluffy.batch.backend.CoordinationBackend;
import com.fluffy.batch.backend.QueueBackend;
import com.fluffy.batch.persistence.JobExecutionRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.batch.core.BatchStatus;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Registers Fluffy Batch metrics with Micrometer for cloud-native observability.
 * <p>
 * Exposed metrics:
 * <ul>
 *   <li>{@code fluffy.batch.queue.depth} — current queue size (gauge)</li>
 *   <li>{@code fluffy.batch.jobs.active} — currently running jobs (gauge)</li>
 *   <li>{@code fluffy.batch.jobs.completed.total} — total completed jobs (counter)</li>
 *   <li>{@code fluffy.batch.jobs.failed.total} — total failed jobs (counter)</li>
 *   <li>{@code fluffy.batch.jobs.stopped.total} — total stopped jobs (counter)</li>
 * </ul>
 */
public class FluffyBatchMetrics {

    private final Counter completedCounter;
    private final Counter failedCounter;
    private final Counter stoppedCounter;

    public FluffyBatchMetrics(MeterRegistry registry,
                              QueueBackend queueBackend,
                              CoordinationBackend coordinationBackend,
                              JobExecutionRepository executionRepository) {

        Gauge.builder("fluffy.batch.queue.depth", queueBackend, QueueBackend::size)
                .description("Current number of jobs in the queue")
                .register(registry);

        Gauge.builder("fluffy.batch.jobs.active", coordinationBackend, CoordinationBackend::getGlobalRunningCount)
                .description("Currently running jobs across all types")
                .register(registry);

        this.completedCounter = Counter.builder("fluffy.batch.jobs.completed.total")
                .description("Total number of completed jobs")
                .register(registry);

        this.failedCounter = Counter.builder("fluffy.batch.jobs.failed.total")
                .description("Total number of failed jobs")
                .register(registry);

        this.stoppedCounter = Counter.builder("fluffy.batch.jobs.stopped.total")
                .description("Total number of stopped jobs")
                .register(registry);

        // Initialize counters from existing database state
        long completed = executionRepository.findByStatus(BatchStatus.COMPLETED).size();
        long failed = executionRepository.findByStatus(BatchStatus.FAILED).size();
        long stopped = executionRepository.findByStatus(BatchStatus.STOPPED).size();
        completedCounter.increment(completed);
        failedCounter.increment(failed);
        stoppedCounter.increment(stopped);
    }

    public void recordCompleted() {
        completedCounter.increment();
    }

    public void recordFailed() {
        failedCounter.increment();
    }

    public void recordStopped() {
        stoppedCounter.increment();
    }
}
