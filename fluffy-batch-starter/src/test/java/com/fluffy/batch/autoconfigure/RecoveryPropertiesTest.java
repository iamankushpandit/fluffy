package com.fluffy.batch.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RecoveryPropertiesTest {

    @Test
    void shouldHaveSensibleDefaults() {
        RecoveryProperties props = new RecoveryProperties();
        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getHeartbeatInterval()).isEqualTo(15);
        assertThat(props.getStaleThreshold()).isEqualTo(60);
    }

    @Test
    void shouldAllowSettingEnabled() {
        RecoveryProperties props = new RecoveryProperties();
        props.setEnabled(true);
        assertThat(props.isEnabled()).isTrue();
    }

    @Test
    void shouldAllowSettingHeartbeatInterval() {
        RecoveryProperties props = new RecoveryProperties();
        props.setHeartbeatInterval(30);
        assertThat(props.getHeartbeatInterval()).isEqualTo(30);
    }

    @Test
    void shouldAllowSettingStaleThreshold() {
        RecoveryProperties props = new RecoveryProperties();
        props.setStaleThreshold(120);
        assertThat(props.getStaleThreshold()).isEqualTo(120);
    }
}
