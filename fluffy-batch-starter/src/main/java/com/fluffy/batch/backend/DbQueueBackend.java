package com.fluffy.batch.backend;

import com.fluffy.batch.model.QueueEntry;
import com.fluffy.batch.persistence.QueueEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Database-backed queue implementation.
 * Persists queue state to a shared database so that it survives restarts and
 * can be shared across multiple application nodes.
 * <p>
 * Uses pessimistic locking ({@code SELECT FOR UPDATE SKIP LOCKED}) on
 * {@link #poll()} to ensure that only one node can claim a given queue entry,
 * preventing duplicate job execution in multi-node deployments.
 * </p>
 */
public class DbQueueBackend implements QueueBackend {

    private static final Logger log = LoggerFactory.getLogger(DbQueueBackend.class);
    private static final String QUEUED = "QUEUED";
    private static final String CLAIMED = "CLAIMED";

    private final QueueEntryRepository queueEntryRepository;
    private final AtomicInteger positionCounter = new AtomicInteger(0);
    private final String nodeId;

    public DbQueueBackend(QueueEntryRepository queueEntryRepository, String nodeId) {
        this.queueEntryRepository = queueEntryRepository;
        this.nodeId = nodeId;
    }

    @Override
    @Transactional
    public void enqueue(Long executionId) {
        int position = positionCounter.incrementAndGet();
        QueueEntry entry = new QueueEntry(executionId, "", position);
        queueEntryRepository.save(entry);
        log.debug("Enqueued execution {} at position {}", executionId, position);
    }

    @Override
    @Transactional
    public Long poll() {
        Optional<QueueEntry> entry = queueEntryRepository.findFirstByStatusForUpdate(QUEUED);
        if (entry.isPresent()) {
            QueueEntry qe = entry.get();
            qe.setStatus(CLAIMED);
            qe.setClaimedBy(nodeId);
            qe.setClaimedAt(Instant.now());
            queueEntryRepository.save(qe);
            log.debug("Polled execution {} from queue (claimed by {})", qe.getExecutionId(), nodeId);
            return qe.getExecutionId();
        }
        return null;
    }

    @Override
    @Transactional
    public boolean remove(Long executionId) {
        int deleted = queueEntryRepository.deleteByExecutionIdAndStatus(executionId, QUEUED);
        if (deleted > 0) {
            log.debug("Removed execution {} from queue", executionId);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getPosition(Long executionId) {
        Optional<QueueEntry> entry = queueEntryRepository.findByExecutionIdAndStatus(executionId, QUEUED);
        return entry.map(QueueEntry::getPosition).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public int size() {
        return (int) queueEntryRepository.countByStatus(QUEUED);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean contains(Long executionId) {
        return queueEntryRepository.existsByExecutionIdAndStatus(executionId, QUEUED);
    }
}
