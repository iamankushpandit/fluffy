package com.fluffy.batch.persistence;

import com.fluffy.batch.model.QueueEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository for persistent queue entries used by the database-backed queue mode.
 */
public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

    /**
     * Finds the oldest QUEUED entry across all job names (FIFO ordering by id).
     */
    Optional<QueueEntry> findFirstByStatusOrderByIdAsc(String status);

    /**
     * Finds all QUEUED entries for a specific execution id.
     */
    Optional<QueueEntry> findByExecutionIdAndStatus(Long executionId, String status);

    /**
     * Finds the entry for a given execution id, regardless of status.
     */
    Optional<QueueEntry> findByExecutionId(Long executionId);

    /**
     * Counts QUEUED entries.
     */
    long countByStatus(String status);

    /**
     * Checks if a queued entry exists for the given execution.
     */
    boolean existsByExecutionIdAndStatus(Long executionId, String status);

    /**
     * Deletes entries for a given execution id.
     */
    @Modifying
    @Query("DELETE FROM QueueEntry q WHERE q.executionId = :executionId AND q.status = :status")
    int deleteByExecutionIdAndStatus(@Param("executionId") Long executionId, @Param("status") String status);
}
