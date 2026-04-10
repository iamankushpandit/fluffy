package com.fluffy.batch.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class MetricsPropertiesTest {

    @Test
    void shouldDefaultToEnabled() {
        MetricsProperties props = new MetricsProperties();
        assertThat(props.isEnabled()).isTrue();
    }

    @Test
    void shouldAllowDisabling() {
        MetricsProperties props = new MetricsProperties();
        props.setEnabled(false);
        assertThat(props.isEnabled()).isFalse();
    }
}
