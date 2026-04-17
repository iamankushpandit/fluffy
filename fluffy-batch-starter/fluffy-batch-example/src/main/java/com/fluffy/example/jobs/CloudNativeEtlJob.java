package com.fluffy.example.jobs;

import com.fluffy.batch.annotation.BatchJob;
import com.fluffy.batch.api.JobContext;
import com.fluffy.batch.api.JobHandler;
import com.fluffy.batch.engine.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demonstrates cloud-native execution mode.
 *
 * <p>When the cloud-native strategy is enabled ({@code fluffy.batch.cloud-native.enabled=true}),
 * this job's execution is dispatched to an external orchestrator (e.g. Kubernetes CronJob,
 * Airflow, AWS Batch) via an HTTP POST instead of running in-process.</p>
 *
 * <p>The handler implementation below acts as a fallback that runs when the job is
 * executed locally — for example in tests, during development, or when the external
 * orchestrator invokes the containerised application with the job's execution context.</p>
 *
 * <p>To try cloud-native dispatch locally, start the app with the {@code cloud-native} profile:</p>
 * <pre>
 *   java -jar fluffy-batch-example.jar --spring.profiles.active=cloud-native
 * </pre>
 */
@BatchJob(
    name = "cloud-native-etl",
    description = "ETL pipeline dispatched to a cloud-native orchestrator (Kubernetes, Airflow, AWS Batch)",
    async = true,
    maxConcurrency = 2,
    timeoutSeconds = 600,
    requiredParams = {"pipeline"},
    executionMode = ExecutionMode.CLOUD_NATIVE
)
public class CloudNativeEtlJob implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(CloudNativeEtlJob.class);

    @Override
    public void execute(JobContext context) throws Exception {
        String pipeline = context.requireParam("pipeline");
        String stage = context.getParam("stage");

        log.info("Cloud-native ETL job started: pipeline={}, stage={}, requestedBy={}",
                pipeline, stage, context.getRequestedBy());

        // Simulate ETL work (this runs when executed locally or by the container)
        int totalSteps = 5;
        for (int i = 0; i < totalSteps; i++) {
            context.checkInterrupted();
            int pct = (int) ((i + 1) * 100.0 / totalSteps);
            log.info("ETL pipeline '{}' progress: {}% (step {}/{})", pipeline, pct, i + 1, totalSteps);
            Thread.sleep(1000);
        }

        log.info("Cloud-native ETL job completed: pipeline={}", pipeline);
    }
}
