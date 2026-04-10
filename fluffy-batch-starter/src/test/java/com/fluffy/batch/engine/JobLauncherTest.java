package com.fluffy.batch.engine;

import com.fluffy.batch.api.JobContext;
import com.fluffy.batch.api.JobRequest;
import com.fluffy.batch.model.JobExecution;
import com.fluffy.batch.model.JobStatus;
import com.fluffy.batch.persistence.JobExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = com.fluffy.batch.TestBatchApplication.class)
class JobLauncherTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private JobRegistry jobRegistry;

    @Autowired
    private JobExecutionRepository executionRepository;

    @BeforeEach
    void setUp() {
        String jobName = "sync-test-job";
        if (!jobRegistry.exists(jobName)) {
            JobDefinition def = JobDefinition.builder(jobName)
                    .async(false)
                    .handler(ctx -> {})
                    .build();
            jobRegistry.register(def);
        }

        String asyncJobName = "async-test-job";
        if (!jobRegistry.exists(asyncJobName)) {
            JobDefinition def = JobDefinition.builder(asyncJobName)
                    .async(true)
                    .handler(ctx -> {
                        Thread.sleep(50);
                    })
                    .build();
            jobRegistry.register(def);
        }

        String stoppableJobName = "stoppable-test-job";
        if (!jobRegistry.exists(stoppableJobName)) {
            JobDefinition def = JobDefinition.builder(stoppableJobName)
                    .async(true)
                    .handler(ctx -> {
                        for (int i = 0; i < 100; i++) {
                            ctx.checkInterrupted();
                            Thread.sleep(50);
                        }
                    })
                    .build();
            jobRegistry.register(def);
        }

        String paramJobName = "param-test-job";
        if (!jobRegistry.exists(paramJobName)) {
            JobDefinition def = JobDefinition.builder(paramJobName)
                    .async(false)
                    .requiredParams("key1")
                    .handler(ctx -> {})
                    .build();
            jobRegistry.register(def);
        }
    }

    @Test
    void shouldLaunchSyncJob() {
        JobRequest request = new JobRequest(null, null, "test-user");

        Long executionId = jobLauncher.launch("sync-test-job", request);

        assertThat(executionId).isNotNull();
        JobExecution execution = executionRepository.findById(executionId).orElseThrow();
        assertThat(execution.getStatus()).isEqualTo(JobStatus.SUCCESS);
        assertThat(execution.getRequestedBy()).isEqualTo("test-user");
    }

    @Test
    void shouldLaunchAsyncJob() throws InterruptedException {
        JobRequest request = new JobRequest(null, null, "test-user");

        Long executionId = jobLauncher.launch("async-test-job", request);
        assertThat(executionId).isNotNull();

        Thread.sleep(500);

        JobExecution execution = executionRepository.findById(executionId).orElseThrow();
        assertThat(execution.getStatus()).isIn(JobStatus.SUCCESS, JobStatus.IN_PROGRESS, JobStatus.STARTED);
    }

    @Test
    void shouldFailWhenMissingRequiredParam() {
        JobRequest request = new JobRequest();
        assertThatThrownBy(() -> jobLauncher.launch("param-test-job", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("key1");
    }

    @Test
    void shouldSucceedWithRequiredParam() {
        JobRequest request = new JobRequest(Map.of("key1", "value1"), null, null);

        Long executionId = jobLauncher.launch("param-test-job", request);
        JobExecution execution = executionRepository.findById(executionId).orElseThrow();
        assertThat(execution.getStatus()).isEqualTo(JobStatus.SUCCESS);
    }

    @Test
    void shouldStopRunningJob() throws InterruptedException {
        JobRequest request = new JobRequest();
        Long executionId = jobLauncher.launch("stoppable-test-job", request);

        Thread.sleep(100);

        boolean stopped = jobLauncher.stop(executionId);
        assertThat(stopped).isTrue();

        Thread.sleep(300);

        JobExecution execution = executionRepository.findById(executionId).orElseThrow();
        assertThat(execution.getStatus()).isIn(JobStatus.STOPPED, JobStatus.IN_PROGRESS, JobStatus.STARTED);
    }

    @Test
    void shouldRetryCompletedJob() {
        JobRequest request = new JobRequest();
        Long executionId = jobLauncher.launch("sync-test-job", request);

        JobExecution execution = executionRepository.findById(executionId).orElseThrow();
        assertThat(execution.getStatus()).isEqualTo(JobStatus.SUCCESS);

        Long retryId = jobLauncher.retry(executionId);
        assertThat(retryId).isNotNull();
        assertThat(retryId).isNotEqualTo(executionId);

        JobExecution retryExecution = executionRepository.findById(retryId).orElseThrow();
        assertThat(retryExecution.getStatus()).isEqualTo(JobStatus.SUCCESS);
    }
}
