package com.fluffy.example.jobs;

import com.fluffy.batch.annotation.BatchJob;
import com.fluffy.batch.api.JobContext;
import com.fluffy.batch.api.JobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@BatchJob(
    name = "data-sync",
    description = "Synchronizes data between systems",
    async = false,
    maxConcurrency = 1
)
public class DataSyncJob implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(DataSyncJob.class);

    @Override
    public void execute(JobContext context) throws Exception {
        String source = context.getParam("source");
        String destination = context.getParam("destination");

        log.info("Starting data sync from {} to {}", source, destination);
        Thread.sleep(100);
        log.info("Data sync completed");
    }
}
