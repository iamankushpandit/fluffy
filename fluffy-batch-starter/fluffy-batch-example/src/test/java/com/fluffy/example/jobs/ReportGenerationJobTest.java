package com.fluffy.example.jobs;

import com.fluffy.batch.api.JobContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class ReportGenerationJobTest {

    private final ReportGenerationJob job = new ReportGenerationJob();

    @Test
    void shouldExecuteWithRequiredParam() throws Exception {
        JobContext ctx = new JobContext(1L, "report-generation", "test-user",
                Map.of("reportType", "monthly"), null);

        job.execute(ctx);
    }

    @Test
    void shouldExecuteWithAllParams() throws Exception {
        JobContext ctx = new JobContext(2L, "report-generation", "test-user",
                Map.of("reportType", "quarterly", "dateRange", "2024-Q1"), null);

        job.execute(ctx);
    }

    @Test
    void shouldThrowForMissingRequiredParam() {
        JobContext ctx = new JobContext(3L, "report-generation", "test-user", Map.of(), null);

        assertThatThrownBy(() -> job.execute(ctx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reportType");
    }

    @Test
    void shouldRespectStopRequest() {
        JobContext ctx = new JobContext(4L, "report-generation", "test-user",
                Map.of("reportType", "daily"), null);
        ctx.markStopRequested();

        assertThatThrownBy(() -> job.execute(ctx))
                .isInstanceOf(InterruptedException.class);
    }
}
