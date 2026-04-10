package com.fluffy.batch.engine;

import com.fluffy.batch.api.JobRequest;
import com.fluffy.batch.model.JobExecution;
import com.fluffy.batch.persistence.JobExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = com.fluffy.batch.TestBatchApplication.class)
class JobLauncherEdgeCaseTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private JobRegistry jobRegistry;

    @Autowired
    private JobExecutionRepository executionRepository;

    @BeforeEach
    void setUp() {
        String failingJob = "failing-edge-job";
        if (!jobRegistry.exists(failingJob)) {
            jobRegistry.register(JobDefinition.builder(failingJob)
                    .async(false)
                    .handler(ctx -> { throw new RuntimeException("boom"); })
                    .build());
        }

        String concurrentJob = "concurrent-limit-job";
        if (!jobRegistry.exists(concurrentJob)) {
            jobRegistry.register(JobDefinition.builder(concurrentJob)
                    .async(true)
                    .maxConcurrency(1)
                    .handler(ctx -> { Thread.sleep(2000); })
                    .build());
        }

        String noopSync = "noop-sync";
        if (!jobRegistry.exists(noopSync)) {
            jobRegistry.register(JobDefinition.builder(noopSync)
                    .async(false)
                    .handler(ctx -> {})
                    .build());
        }

        String timeoutJob = "timeout-edge-job";
        if (!jobRegistry.exists(timeoutJob)) {
            jobRegistry.register(JobDefinition.builder(timeoutJob)
                    .async(true)
                    .timeoutSeconds(1)
                    .handler(ctx -> {
                        for (int i = 0; i < 100; i++) {
                            ctx.checkInterrupted();
                            Thread.sleep(100);
                        }
                    })
                    .build());
        }

        String multiParamJob = "multi-param-job";
        if (!jobRegistry.exists(multiParamJob)) {
            jobRegistry.register(JobDefinition.builder(multiParamJob)
                    .async(false)
                    .requiredParams("p1", "p2")
                    .handler(ctx -> {})
                    .build());
        }
    }

    @Test
    void shouldLaunchWithNullRequest() {
        Long id = jobLauncher.launch("noop-sync", null);
        assertThat(id).isNotNull();
        JobExecution exec = executionRepository.findById(id).orElseThrow();
        assertThat(exec.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(exec.getRequestedBy()).isEqualTo("anonymous");
    }

    @Test
    void shouldRecordFailure() {
        Long id = jobLauncher.launch("failing-edge-job", new JobRequest());

        JobExecution exec = executionRepository.findById(id).orElseThrow();
        assertThat(exec.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(exec.getErrorMessage()).isEqualTo("boom");
        assertThat(exec.getEndTime()).isNotNull();
    }

    @Test
    void shouldThrowForUnknownJob() {
        assertThatThrownBy(() -> jobLauncher.launch("nonexistent-job", new JobRequest()))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void shouldReturnFalseWhenStoppingNonExistentExecution() {
        boolean stopped = jobLauncher.stop(999999L);
        assertThat(stopped).isFalse();
    }

    @Test
    void shouldThrowWhenRetryingNonExistentExecution() {
        assertThatThrownBy(() -> jobLauncher.retry(999999L))
                .isInstanceOf(JobNotFoundException.class);
    }

    @Test
    void shouldQueueWhenConcurrencyLimitReached() throws InterruptedException {
        // Launch first job that runs for 2 seconds
        Long id1 = jobLauncher.launch("concurrent-limit-job", new JobRequest());
        Thread.sleep(100);

        // Second launch should be queued
        Long id2 = jobLauncher.launch("concurrent-limit-job", new JobRequest());

        JobExecution exec2 = executionRepository.findById(id2).orElseThrow();
        assertThat(exec2.getStatus()).isIn(BatchStatus.STARTING, BatchStatus.STARTED);

        // Wait for both to complete
        Thread.sleep(5000);
    }

    @Test
    void shouldThrowWhenRetryingInProgressJob() throws InterruptedException {
        Long id = jobLauncher.launch("concurrent-limit-job", new JobRequest());
        Thread.sleep(100);

        assertThatThrownBy(() -> jobLauncher.retry(id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("still running or queued");

        // Clean up
        jobLauncher.stop(id);
        Thread.sleep(500);
    }

    @Test
    void shouldThrowForMissingMultipleRequiredParams() {
        assertThatThrownBy(() -> jobLauncher.launch("multi-param-job", new JobRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing required parameter");
    }

    @Test
    void shouldSucceedWithAllRequiredParams() {
        JobRequest req = new JobRequest(Map.of("p1", "v1", "p2", "v2"), null, "user1");
        Long id = jobLauncher.launch("multi-param-job", req);

        JobExecution exec = executionRepository.findById(id).orElseThrow();
        assertThat(exec.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    void shouldThrowWhenRequiredParamIsMissing() {
        // Only provide p1 but not p2
        JobRequest req = new JobRequest(Map.of("p1", "v1"), null, null);

        assertThatThrownBy(() -> jobLauncher.launch("multi-param-job", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("p2");
    }

    @Test
    void shouldStopQueuedJob() throws InterruptedException {
        // Fill concurrency slot
        Long id1 = jobLauncher.launch("concurrent-limit-job", new JobRequest());
        Thread.sleep(100);

        // This one should be queued
        Long id2 = jobLauncher.launch("concurrent-limit-job", new JobRequest());
        Thread.sleep(100);

        // Stop the queued job
        boolean stopped = jobLauncher.stop(id2);
        assertThat(stopped).isTrue();

        JobExecution exec2 = executionRepository.findById(id2).orElseThrow();
        assertThat(exec2.getStatus()).isEqualTo(BatchStatus.STOPPED);

        // Clean up first job
        jobLauncher.stop(id1);
        Thread.sleep(500);
    }

    @Test
    void shouldHandleTimeoutJob() throws InterruptedException {
        Long id = jobLauncher.launch("timeout-edge-job", new JobRequest());
        // Wait for timeout to trigger (1 second timeout + some buffer)
        Thread.sleep(3000);

        JobExecution exec = executionRepository.findById(id).orElseThrow();
        assertThat(exec.getStatus()).isIn(BatchStatus.STOPPED, BatchStatus.FAILED);
        assertThat(exec.getEndTime()).isNotNull();
    }

    @Test
    void shouldLaunchWithArguments() {
        JobRequest req = new JobRequest(null, "my-arguments", "user1");
        Long id = jobLauncher.launch("noop-sync", req);

        JobExecution exec = executionRepository.findById(id).orElseThrow();
        assertThat(exec.getArguments()).isEqualTo("my-arguments");
    }

    @Test
    void shouldLaunchWithParameters() {
        JobRequest req = new JobRequest(Map.of("k1", "v1"), null, null);
        Long id = jobLauncher.launch("noop-sync", req);

        JobExecution exec = executionRepository.findById(id).orElseThrow();
        assertThat(exec.getParameters()).isNotNull();
        assertThat(exec.getParameters()).contains("k1");
    }

    @Test
    void shouldTruncateLongErrorMessage() {
        String longErrorJob = "long-error-job";
        if (!jobRegistry.exists(longErrorJob)) {
            jobRegistry.register(JobDefinition.builder(longErrorJob)
                    .async(false)
                    .handler(ctx -> { throw new RuntimeException("x".repeat(5000)); })
                    .build());
        }

        Long id = jobLauncher.launch(longErrorJob, new JobRequest());
        JobExecution exec = executionRepository.findById(id).orElseThrow();
        assertThat(exec.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(exec.getErrorMessage()).hasSize(4096);
    }

    @Test
    void shouldRetryJobWithInvalidStoredParameters() {
        // Launch a job, then corrupt its stored parameters, then retry
        Long id = jobLauncher.launch("noop-sync", new JobRequest());
        JobExecution exec = executionRepository.findById(id).orElseThrow();
        assertThat(exec.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Corrupt the stored parameters with invalid JSON
        exec.setParameters("not-valid-json{{{");
        executionRepository.save(exec);

        // Retry should still work — stringToParams fallback returns empty map
        Long retryId = jobLauncher.retry(id);
        assertThat(retryId).isNotNull();
    }
}
