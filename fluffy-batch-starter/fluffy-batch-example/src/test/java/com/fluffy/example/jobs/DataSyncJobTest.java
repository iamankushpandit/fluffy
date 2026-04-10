package com.fluffy.example.jobs;

import com.fluffy.batch.api.JobContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class DataSyncJobTest {

    private final DataSyncJob job = new DataSyncJob();

    @Test
    void shouldExecuteSuccessfully() throws Exception {
        JobContext ctx = new JobContext(1L, "data-sync", "test-user",
                Map.of("source", "systemA", "destination", "systemB"), null);

        job.execute(ctx);
        // If we reach here, the job completed without error
    }

    @Test
    void shouldHandleNullParams() throws Exception {
        JobContext ctx = new JobContext(2L, "data-sync", "test-user", null, null);

        job.execute(ctx);
    }

    @Test
    void shouldHandleEmptyParams() throws Exception {
        JobContext ctx = new JobContext(3L, "data-sync", "test-user", Map.of(), null);

        job.execute(ctx);
    }
}
