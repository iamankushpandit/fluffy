package com.fluffy.example.jobs;

import com.fluffy.batch.annotation.BatchJob;
import com.fluffy.batch.api.JobContext;
import com.fluffy.batch.api.JobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@BatchJob(
    name = "report-generation",
    description = "Generates a report based on provided parameters",
    async = true,
    maxConcurrency = 2,
    timeoutSeconds = 360,
    requiredParams = {"reportType"}
)
public class ReportGenerationJob implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(ReportGenerationJob.class);

    @Override
    public void execute(JobContext context) throws Exception {
        String reportType = context.requireParam("reportType");
        String dateRange = context.getParam("dateRange");

        log.info("Generating report: type={}, dateRange={}, requestedBy={}",
                reportType, dateRange, context.getRequestedBy());

        int totalIterations = 60;
        for (int i = 0; i < totalIterations; i++) {
            context.checkInterrupted();
            int pct = (int) ((i + 1) * 100.0 / totalIterations);
            log.info("Report generation progress: {}% (iteration {}/{})", pct, i + 1, totalIterations);
            Thread.sleep(5000);
        }

        log.info("Report generation completed successfully");
    }
}
