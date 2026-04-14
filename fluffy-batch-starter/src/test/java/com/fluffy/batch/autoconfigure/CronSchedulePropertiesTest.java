package com.fluffy.batch.autoconfigure;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class CronSchedulePropertiesTest {

    @Test
    void shouldHaveSensibleDefaults() {
        CronScheduleProperties props = new CronScheduleProperties();
        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getSchedules()).isEmpty();
    }

    @Test
    void shouldAllowSettingEnabled() {
        CronScheduleProperties props = new CronScheduleProperties();
        props.setEnabled(true);
        assertThat(props.isEnabled()).isTrue();
    }

    @Test
    void shouldAllowSettingSchedules() {
        CronScheduleProperties props = new CronScheduleProperties();
        props.setSchedules(Map.of("data-sync", "0 0 * * * *", "report", "0 30 2 * * *"));
        assertThat(props.getSchedules()).hasSize(2);
        assertThat(props.getSchedules()).containsEntry("data-sync", "0 0 * * * *");
        assertThat(props.getSchedules()).containsEntry("report", "0 30 2 * * *");
    }
}
