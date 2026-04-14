package com.fluffy.batch.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class CronScheduleTest {

    @Test
    void shouldSetAndGetAllFields() {
        CronSchedule schedule = new CronSchedule();
        Instant now = Instant.now();

        schedule.setId(1L);
        schedule.setJobName("data-sync");
        schedule.setCronExpression("0 0 * * * *");
        schedule.setTargetNode("node-1");
        schedule.setEnabled(true);
        schedule.setCreatedAt(now);
        schedule.setLastTriggeredAt(now);

        assertThat(schedule.getId()).isEqualTo(1L);
        assertThat(schedule.getJobName()).isEqualTo("data-sync");
        assertThat(schedule.getCronExpression()).isEqualTo("0 0 * * * *");
        assertThat(schedule.getTargetNode()).isEqualTo("node-1");
        assertThat(schedule.isEnabled()).isTrue();
        assertThat(schedule.getCreatedAt()).isEqualTo(now);
        assertThat(schedule.getLastTriggeredAt()).isEqualTo(now);
    }

    @Test
    void shouldHandleDefaultValues() {
        CronSchedule schedule = new CronSchedule();

        assertThat(schedule.getId()).isNull();
        assertThat(schedule.getJobName()).isNull();
        assertThat(schedule.getCronExpression()).isNull();
        assertThat(schedule.getTargetNode()).isNull();
        assertThat(schedule.isEnabled()).isTrue();
        assertThat(schedule.getCreatedAt()).isNull();
        assertThat(schedule.getLastTriggeredAt()).isNull();
    }

    @Test
    void shouldCreateWithConstructor() {
        CronSchedule schedule = new CronSchedule("my-job", "0 30 2 * * *");

        assertThat(schedule.getJobName()).isEqualTo("my-job");
        assertThat(schedule.getCronExpression()).isEqualTo("0 30 2 * * *");
        assertThat(schedule.isEnabled()).isTrue();
        assertThat(schedule.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldToggleEnabled() {
        CronSchedule schedule = new CronSchedule("my-job", "0 0 * * * *");
        assertThat(schedule.isEnabled()).isTrue();

        schedule.setEnabled(false);
        assertThat(schedule.isEnabled()).isFalse();
    }
}
