package com.fluffy.batch.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ScalingPropertiesTest {

    @Test
    void shouldHaveSensibleDefaults() {
        ScalingProperties props = new ScalingProperties();
        assertThat(props.getMaxQueueDepth()).isEqualTo(100);
        assertThat(props.isRuntimeUpdatesEnabled()).isTrue();
    }

    @Test
    void shouldAllowSettingMaxQueueDepth() {
        ScalingProperties props = new ScalingProperties();
        props.setMaxQueueDepth(200);
        assertThat(props.getMaxQueueDepth()).isEqualTo(200);
    }

    @Test
    void shouldAllowDisablingRuntimeUpdates() {
        ScalingProperties props = new ScalingProperties();
        props.setRuntimeUpdatesEnabled(false);
        assertThat(props.isRuntimeUpdatesEnabled()).isFalse();
    }
}
