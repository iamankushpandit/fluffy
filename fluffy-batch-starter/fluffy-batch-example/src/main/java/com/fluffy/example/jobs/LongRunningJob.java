package com.fluffy.example.jobs;

import com.fluffy.batch.annotation.BatchJob;
import com.fluffy.batch.api.JobContext;
import com.fluffy.batch.api.JobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@BatchJob(
    name = "long-running",
    description = "Demonstrates stop support with checkInterrupted",
    async = true,
    maxConcurrency = 3,
    timeoutSeconds = 300
)
public class LongRunningJob implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(LongRunningJob.class);

    @Override
    public void execute(JobContext context) throws Exception {
        log.info("Long running job started, executionId={}", context.getExecutionId());

        for (int i = 0; i < 60; i++) {
            context.checkInterrupted();
            log.info("Long running job iteration {}/60", i + 1);
            Thread.sleep(1000);
        }

        log.info("Long running job completed");
    }
}
