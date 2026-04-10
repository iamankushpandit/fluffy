package com.fluffy.batch.api;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class JobContextTest {

    @Test
    void shouldCreateContextWithAllFields() {
        Map<String, String> params = Map.of("key1", "value1");
        JobContext ctx = new JobContext(1L, "test-job", "user1", params, "args");

        assertThat(ctx.getExecutionId()).isEqualTo(1L);
        assertThat(ctx.getJobName()).isEqualTo("test-job");
        assertThat(ctx.getRequestedBy()).isEqualTo("user1");
        assertThat(ctx.getParameters()).containsEntry("key1", "value1");
        assertThat(ctx.getArguments()).isEqualTo("args");
        assertThat(ctx.isStopRequested()).isFalse();
    }

    @Test
    void shouldCreateEmptyParametersWhenNull() {
        JobContext ctx = new JobContext(1L, "test-job", "user1", null, null);

        assertThat(ctx.getParameters()).isNotNull().isEmpty();
        assertThat(ctx.getArguments()).isNull();
    }

    @Test
    void shouldReturnUnmodifiableParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("key1", "value1");
        JobContext ctx = new JobContext(1L, "test-job", "user1", params, null);

        assertThatThrownBy(() -> ctx.getParameters().put("key2", "value2"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldGetParamWhenPresent() {
        Map<String, String> params = Map.of("key1", "value1");
        JobContext ctx = new JobContext(1L, "test-job", "user1", params, null);

        assertThat(ctx.getParam("key1")).isEqualTo("value1");
    }

    @Test
    void shouldReturnNullForMissingParam() {
        JobContext ctx = new JobContext(1L, "test-job", "user1", Map.of(), null);

        assertThat(ctx.getParam("missing")).isNull();
    }

    @Test
    void shouldRequireParamWhenPresent() {
        Map<String, String> params = Map.of("key1", "value1");
        JobContext ctx = new JobContext(1L, "test-job", "user1", params, null);

        assertThat(ctx.requireParam("key1")).isEqualTo("value1");
    }

    @Test
    void shouldThrowForMissingRequiredParam() {
        JobContext ctx = new JobContext(1L, "test-job", "user1", Map.of(), null);

        assertThatThrownBy(() -> ctx.requireParam("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void shouldMarkStopRequested() {
        JobContext ctx = new JobContext(1L, "test-job", "user1", null, null);

        assertThat(ctx.isStopRequested()).isFalse();
        ctx.markStopRequested();
        assertThat(ctx.isStopRequested()).isTrue();
    }

    @Test
    void checkInterruptedShouldNotThrowWhenNotStopped() throws InterruptedException {
        JobContext ctx = new JobContext(1L, "test-job", "user1", null, null);
        ctx.checkInterrupted(); // should not throw
    }

    @Test
    void checkInterruptedShouldThrowWhenStopRequested() {
        JobContext ctx = new JobContext(1L, "test-job", "user1", null, null);
        ctx.markStopRequested();

        assertThatThrownBy(ctx::checkInterrupted)
                .isInstanceOf(InterruptedException.class)
                .hasMessageContaining("Job stop requested");
    }

    @Test
    void checkInterruptedShouldThrowWhenThreadInterrupted() {
        JobContext ctx = new JobContext(1L, "test-job", "user1", null, null);
        Thread.currentThread().interrupt();

        assertThatThrownBy(ctx::checkInterrupted)
                .isInstanceOf(InterruptedException.class);

        // clear interrupted status
        Thread.interrupted();
    }
}
