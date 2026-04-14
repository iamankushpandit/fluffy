package com.fluffy.example.jobs;

import com.fluffy.batch.annotation.BatchJob;
import com.fluffy.batch.api.JobContext;
import com.fluffy.batch.api.JobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@BatchJob(
    name = "data-sync",
    description = "Synchronizes data between systems",
    async = true,
    maxConcurrency = 1,
    timeoutSeconds = 360
)
public class DataSyncJob implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(DataSyncJob.class);

    @Override
    public void execute(JobContext context) throws Exception {
        String source = context.getParam("source");
        String destination = context.getParam("destination");

        log.info("Starting data sync from {} to {}", source, destination);

        int totalIterations = 60;
        for (int i = 0; i < totalIterations; i++) {
            context.checkInterrupted();
            log.info("Data sync progress: iteration {}/{}", i + 1, totalIterations);
            Thread.sleep(5000);
        }

        log.info("Data sync completed");
    }
}
