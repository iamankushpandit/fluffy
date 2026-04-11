package com.fluffy.batch.backend;

import com.fluffy.batch.model.QueueEntry;
import com.fluffy.batch.persistence.QueueEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Database-backed queue implementation.
 * Persists queue state to a shared database so that it survives restarts and
 * can be shared across multiple application nodes.
 */
public class DbQueueBackend implements QueueBackend {

    private static final Logger log = LoggerFactory.getLogger(DbQueueBackend.class);
    private static final String QUEUED = "QUEUED";
    private static final String CLAIMED = "CLAIMED";

    private final QueueEntryRepository queueEntryRepository;
    private final AtomicInteger positionCounter = new AtomicInteger(0);

    public DbQueueBackend(QueueEntryRepository queueEntryRepository) {
        this.queueEntryRepository = queueEntryRepository;
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
        Optional<QueueEntry> entry = queueEntryRepository.findFirstByStatusOrderByIdAsc(QUEUED);
        if (entry.isPresent()) {
            QueueEntry qe = entry.get();
            qe.setStatus(CLAIMED);
            queueEntryRepository.save(qe);
            log.debug("Polled execution {} from queue", qe.getExecutionId());
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
