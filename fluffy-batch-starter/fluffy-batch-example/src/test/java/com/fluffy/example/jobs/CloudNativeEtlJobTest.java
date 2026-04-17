package com.fluffy.example.jobs;

import com.fluffy.batch.api.JobContext;
import com.fluffy.batch.engine.ExecutionMode;
import com.fluffy.batch.annotation.BatchJob;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class CloudNativeEtlJobTest {

    private final CloudNativeEtlJob job = new CloudNativeEtlJob();

    @Test
    void shouldExecuteWithRequiredParam() throws Exception {
        JobContext ctx = new JobContext(1L, "cloud-native-etl", "test-user",
                Map.of("pipeline", "daily-ingest"), null);

        job.execute(ctx);
    }

    @Test
    void shouldExecuteWithAllParams() throws Exception {
        JobContext ctx = new JobContext(2L, "cloud-native-etl", "test-user",
                Map.of("pipeline", "weekly-report", "stage", "transform"), null);

        job.execute(ctx);
    }

    @Test
    void shouldThrowForMissingRequiredParam() {
        JobContext ctx = new JobContext(3L, "cloud-native-etl", "test-user", Map.of(), null);

        assertThatThrownBy(() -> job.execute(ctx))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pipeline");
    }

    @Test
    void shouldRespectStopRequest() {
        JobContext ctx = new JobContext(4L, "cloud-native-etl", "test-user",
                Map.of("pipeline", "stopped-pipeline"), null);
        ctx.markStopRequested();

        assertThatThrownBy(() -> job.execute(ctx))
                .isInstanceOf(InterruptedException.class);
    }

    @Test
    void shouldBeAnnotatedAsCloudNative() {
        BatchJob annotation = CloudNativeEtlJob.class.getAnnotation(BatchJob.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.name()).isEqualTo("cloud-native-etl");
        assertThat(annotation.executionMode()).isEqualTo(ExecutionMode.CLOUD_NATIVE);
        assertThat(annotation.requiredParams()).containsExactly("pipeline");
        assertThat(annotation.maxConcurrency()).isEqualTo(2);
    }
}
