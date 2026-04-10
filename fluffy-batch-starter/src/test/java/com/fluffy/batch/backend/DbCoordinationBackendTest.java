package com.fluffy.batch.backend;

import com.fluffy.batch.model.JobExecution;
import com.fluffy.batch.persistence.JobExecutionRepository;
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
class DbCoordinationBackendTest {

    @Autowired
    private JobExecutionRepository executionRepository;

    private DbCoordinationBackend coordinationBackend;

    @BeforeEach
    void setUp() {
        coordinationBackend = new DbCoordinationBackend(executionRepository);
    }

    @Test
    void shouldAllowRunWhenBelowLimit() {
        assertThat(coordinationBackend.canRun("testjob1", 3)).isTrue();
    }

    @Test
    void shouldDenyRunWhenAtLimit() {
        createExecution("denytest", BatchStatus.STARTED);
        createExecution("denytest", BatchStatus.STARTED);
        assertThat(coordinationBackend.canRun("denytest", 2)).isFalse();
    }

    @Test
    void shouldReturnRunningCount() {
        createExecution("counttest", BatchStatus.STARTED);
        createExecution("counttest", BatchStatus.COMPLETED);
        createExecution("counttest", BatchStatus.STARTED);

        assertThat(coordinationBackend.getRunningCount("counttest")).isEqualTo(2);
    }

    @Test
    void shouldReturnGlobalRunningCount() {
        createExecution("globaltest1", BatchStatus.STARTED);
        createExecution("globaltest2", BatchStatus.STARTED);
        createExecution("globaltest3", BatchStatus.COMPLETED);

        assertThat(coordinationBackend.getGlobalRunningCount()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldCountStartingAsRunning() {
        createExecution("startingtest", BatchStatus.STARTING);
        assertThat(coordinationBackend.getRunningCount("startingtest")).isEqualTo(1);
    }

    @Test
    void incrementShouldBeNoOp() {
        coordinationBackend.increment("noop1");
        // No error thrown; running count is from DB
    }

    @Test
    void decrementShouldBeNoOp() {
        coordinationBackend.decrement("noop2");
        // No error thrown; running count is from DB
    }

    private void createExecution(String jobName, BatchStatus status) {
        JobExecution exec = new JobExecution();
        exec.setJobName(jobName);
        exec.setStatus(status);
        exec.setStartTime(Instant.now());
        exec.setRequestedBy("test");
        executionRepository.save(exec);
    }
}
