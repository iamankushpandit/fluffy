package com.fluffy.batch.persistence;

import com.fluffy.batch.model.NodeHeartbeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for node heartbeat tracking in multi-node environments.
 */
public interface NodeHeartbeatRepository extends JpaRepository<NodeHeartbeat, String> {

    /**
     * Finds all nodes whose last heartbeat is before the given threshold.
     */
    List<NodeHeartbeat> findByLastHeartbeatBeforeAndStatus(Instant threshold, String status);
}
