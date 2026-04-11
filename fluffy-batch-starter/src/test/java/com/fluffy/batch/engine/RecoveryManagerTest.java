package com.fluffy.batch.engine;

import com.fluffy.batch.autoconfigure.RecoveryProperties;
import com.fluffy.batch.model.JobExecution;
import com.fluffy.batch.model.NodeHeartbeat;
import com.fluffy.batch.persistence.JobExecutionRepository;
import com.fluffy.batch.persistence.NodeHeartbeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = com.fluffy.batch.TestBatchApplication.class)
@Transactional
class RecoveryManagerTest {

    @Autowired
    private NodeHeartbeatRepository heartbeatRepository;

    @Autowired
    private JobExecutionRepository executionRepository;

    private RecoveryManager recoveryManager;
    private RecoveryProperties properties;

    @BeforeEach
    void setUp() {
        properties = new RecoveryProperties();
        properties.setEnabled(true);
        properties.setHeartbeatInterval(15);
        properties.setStaleThreshold(60);
        recoveryManager = new RecoveryManager("test-node", properties, heartbeatRepository, executionRepository);
    }

    @Test
    void shouldSendHeartbeat() {
        recoveryManager.sendHeartbeat();

        NodeHeartbeat heartbeat = heartbeatRepository.findById("test-node").orElseThrow();
        assertThat(heartbeat.getStatus()).isEqualTo("ALIVE");
        assertThat(heartbeat.getLastHeartbeat()).isNotNull();
    }

    @Test
    void shouldUpdateExistingHeartbeat() {
        recoveryManager.sendHeartbeat();
        Instant firstHeartbeat = heartbeatRepository.findById("test-node").orElseThrow().getLastHeartbeat();

        recoveryManager.sendHeartbeat();
        Instant secondHeartbeat = heartbeatRepository.findById("test-node").orElseThrow().getLastHeartbeat();

        assertThat(secondHeartbeat).isAfterOrEqualTo(firstHeartbeat);
    }

    @Test
    void shouldRecoverJobsFromStaleNode() {
        // Create a stale node heartbeat
        NodeHeartbeat staleNode = new NodeHeartbeat("stale-node");
        staleNode.setLastHeartbeat(Instant.now().minusSeconds(120));
        heartbeatRepository.save(staleNode);

        // Create an orphaned running job
        JobExecution orphanedJob = new JobExecution();
        orphanedJob.setJobName("orphaned-job");
        orphanedJob.setStatus(BatchStatus.STARTED);
        orphanedJob.setOwnerNode("stale-node");
        orphanedJob.setStartTime(Instant.now().minusSeconds(120));
        orphanedJob.setRequestedBy("test");
        executionRepository.save(orphanedJob);

        // Run recovery
        recoveryManager.recoverStaleNodes();

        // Verify the job was marked as failed
        JobExecution recovered = executionRepository.findById(orphanedJob.getId()).orElseThrow();
        assertThat(recovered.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(recovered.getErrorMessage()).contains("Node lost");
        assertThat(recovered.getEndTime()).isNotNull();

        // Verify the stale node is marked as STALE
        NodeHeartbeat updatedStaleNode = heartbeatRepository.findById("stale-node").orElseThrow();
        assertThat(updatedStaleNode.getStatus()).isEqualTo("STALE");
    }

    @Test
    void shouldRecoverStartingJobsFromStaleNode() {
        NodeHeartbeat staleNode = new NodeHeartbeat("stale-node-2");
        staleNode.setLastHeartbeat(Instant.now().minusSeconds(120));
        heartbeatRepository.save(staleNode);

        JobExecution startingJob = new JobExecution();
        startingJob.setJobName("starting-job");
        startingJob.setStatus(BatchStatus.STARTING);
        startingJob.setOwnerNode("stale-node-2");
        startingJob.setStartTime(Instant.now().minusSeconds(120));
        startingJob.setRequestedBy("test");
        executionRepository.save(startingJob);

        recoveryManager.recoverStaleNodes();

        JobExecution recovered = executionRepository.findById(startingJob.getId()).orElseThrow();
        assertThat(recovered.getStatus()).isEqualTo(BatchStatus.FAILED);
    }

    @Test
    void shouldNotRecoverActiveNodes() {
        // Create an active node heartbeat (recent)
        NodeHeartbeat activeNode = new NodeHeartbeat("active-node");
        activeNode.setLastHeartbeat(Instant.now());
        heartbeatRepository.save(activeNode);

        JobExecution runningJob = new JobExecution();
        runningJob.setJobName("active-job");
        runningJob.setStatus(BatchStatus.STARTED);
        runningJob.setOwnerNode("active-node");
        runningJob.setStartTime(Instant.now());
        runningJob.setRequestedBy("test");
        executionRepository.save(runningJob);

        recoveryManager.recoverStaleNodes();

        JobExecution stillRunning = executionRepository.findById(runningJob.getId()).orElseThrow();
        assertThat(stillRunning.getStatus()).isEqualTo(BatchStatus.STARTED);
    }

    @Test
    void shouldReturnNodeId() {
        assertThat(recoveryManager.getNodeId()).isEqualTo("test-node");
    }
}
