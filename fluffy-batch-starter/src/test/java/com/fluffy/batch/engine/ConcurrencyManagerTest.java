package com.fluffy.batch.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ConcurrencyManagerTest {

    private ConcurrencyManager manager;

    @BeforeEach
    void setUp() {
        manager = new ConcurrencyManager();
    }

    @Test
    void shouldAllowRunWhenBelowLimit() {
        assertThat(manager.canRun("job1", 3)).isTrue();
    }

    @Test
    void shouldDenyRunWhenAtLimit() {
        manager.increment("job1");
        manager.increment("job1");
        assertThat(manager.canRun("job1", 2)).isFalse();
    }

    @Test
    void shouldAllowRunAfterDecrement() {
        manager.increment("job1");
        manager.increment("job1");
        assertThat(manager.canRun("job1", 2)).isFalse();
        manager.decrement("job1");
        assertThat(manager.canRun("job1", 2)).isTrue();
    }

    @Test
    void shouldTrackPerJobCounters() {
        manager.increment("job1");
        manager.increment("job1");
        manager.increment("job2");
        assertThat(manager.getRunningCount("job1")).isEqualTo(2);
        assertThat(manager.getRunningCount("job2")).isEqualTo(1);
    }

    @Test
    void shouldReturnZeroForUnknownJob() {
        assertThat(manager.getRunningCount("unknown")).isEqualTo(0);
    }

    @Test
    void shouldTrackGlobalCount() {
        manager.increment("job1");
        manager.increment("job2");
        assertThat(manager.getGlobalRunningCount()).isEqualTo(2);
    }

    @Test
    void shouldDecrementGlobalCount() {
        manager.increment("job1");
        manager.increment("job2");
        manager.decrement("job1");
        assertThat(manager.getGlobalRunningCount()).isEqualTo(1);
    }

    @Test
    void shouldHandleDecrementForUnknownJob() {
        // decrement on a job that was never incremented should be a no-op
        manager.decrement("unknown-job");
        assertThat(manager.getGlobalRunningCount()).isEqualTo(0);
    }
}
