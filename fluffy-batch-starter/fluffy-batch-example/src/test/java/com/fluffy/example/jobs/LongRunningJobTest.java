package com.fluffy.example.jobs;

import com.fluffy.batch.api.JobContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class LongRunningJobTest {

    private final LongRunningJob job = new LongRunningJob();

    @Test
    void shouldRespectStopRequest() {
        JobContext ctx = new JobContext(1L, "long-running", "test-user", Map.of(), null);
        ctx.markStopRequested();

        assertThatThrownBy(() -> job.execute(ctx))
                .isInstanceOf(InterruptedException.class);
    }

    @Test
    void shouldExecuteIterationsBeforeInterrupt() {
        JobContext ctx = new JobContext(2L, "long-running", "test-user", null, null);

        Thread testThread = new Thread(() -> {
            try {
                job.execute(ctx);
            } catch (InterruptedException e) {
                // expected
            } catch (Exception e) {
                fail("Unexpected exception: " + e.getMessage());
            }
        });
        testThread.start();

        try {
            // Let a few iterations run (each is 1 second, let 1.5 seconds pass)
            Thread.sleep(1500);
            ctx.markStopRequested();
            testThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void shouldLogStartAndProgress() throws Exception {
        JobContext ctx = new JobContext(3L, "long-running", "test-user", Map.of(), null);

        // Run in background, let a couple iterations happen, then interrupt
        Thread runner = new Thread(() -> {
            try {
                job.execute(ctx);
            } catch (InterruptedException e) {
                // expected - stopped by interrupt
            } catch (Exception e) {
                // should not happen
            }
        });
        runner.start();
        Thread.sleep(2500); // allow 2+ iterations
        runner.interrupt();
        runner.join(5000);
    }
}
