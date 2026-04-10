package com.fluffy.batch.engine;

import com.fluffy.batch.autoconfigure.RecoveryProperties;
import com.fluffy.batch.model.JobExecution;
import com.fluffy.batch.model.NodeHeartbeat;
import com.fluffy.batch.persistence.JobExecutionRepository;
import com.fluffy.batch.persistence.NodeHeartbeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Manages node heartbeats and recovers jobs from stale nodes.
 * <p>
 * Periodically updates this node's heartbeat and checks for nodes that have
 * gone stale (no heartbeat within the configured threshold). Jobs owned by
 * stale nodes are marked as FAILED so they can be retried.
 * </p>
 */
public class RecoveryManager {

    private static final Logger log = LoggerFactory.getLogger(RecoveryManager.class);

    private final String nodeId;
    private final RecoveryProperties properties;
    private final NodeHeartbeatRepository heartbeatRepository;
    private final JobExecutionRepository executionRepository;

    public RecoveryManager(String nodeId,
                           RecoveryProperties properties,
                           NodeHeartbeatRepository heartbeatRepository,
                           JobExecutionRepository executionRepository) {
        this.nodeId = nodeId;
        this.properties = properties;
        this.heartbeatRepository = heartbeatRepository;
        this.executionRepository = executionRepository;
    }

    /**
     * Updates this node's heartbeat in the database.
     */
    @Transactional
    public void sendHeartbeat() {
        NodeHeartbeat heartbeat = heartbeatRepository.findById(nodeId)
                .orElse(new NodeHeartbeat(nodeId));
        heartbeat.setLastHeartbeat(Instant.now());
        heartbeat.setStatus("ALIVE");
        heartbeatRepository.save(heartbeat);
        log.trace("Heartbeat updated for node {}", nodeId);
    }

    /**
     * Checks for stale nodes and marks their owned jobs as FAILED.
     */
    @Transactional
    public void recoverStaleNodes() {
        Instant threshold = Instant.now().minusSeconds(properties.getStaleThreshold());
        List<NodeHeartbeat> staleNodes = heartbeatRepository.findByLastHeartbeatBeforeAndStatus(threshold, "ALIVE");

        for (NodeHeartbeat staleNode : staleNodes) {
            log.warn("Detected stale node: {}", staleNode.getNodeId());

            List<JobExecution> orphanedJobs = executionRepository.findByOwnerNodeAndStatus(staleNode.getNodeId(), BatchStatus.STARTED);
            for (JobExecution job : orphanedJobs) {
                job.setStatus(BatchStatus.FAILED);
                job.setErrorMessage("Node lost: " + staleNode.getNodeId());
                job.setEndTime(Instant.now());
                executionRepository.save(job);
                log.warn("Marked job {} as FAILED due to stale node {}", job.getId(), staleNode.getNodeId());
            }

            List<JobExecution> startingJobs = executionRepository.findByOwnerNodeAndStatus(staleNode.getNodeId(), BatchStatus.STARTING);
            for (JobExecution job : startingJobs) {
                job.setStatus(BatchStatus.FAILED);
                job.setErrorMessage("Node lost: " + staleNode.getNodeId());
                job.setEndTime(Instant.now());
                executionRepository.save(job);
                log.warn("Marked starting job {} as FAILED due to stale node {}", job.getId(), staleNode.getNodeId());
            }

            staleNode.setStatus("STALE");
            heartbeatRepository.save(staleNode);
        }
    }

    public String getNodeId() {
        return nodeId;
    }
}
