package com.fluffy.batch.dashboard;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class DashboardPropertiesTest {

    @Test
    void shouldHaveDefaultValues() {
        DashboardProperties props = new DashboardProperties();

        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getPath()).isEqualTo("/fluffy-dashboard");
        assertThat(props.getTitle()).isEqualTo("Fluffy Batch Dashboard");
        assertThat(props.getRefreshInterval()).isEqualTo(5);
        assertThat(props.isAuthEnabled()).isFalse();
    }

    @Test
    void shouldSetAndGetAllProperties() {
        DashboardProperties props = new DashboardProperties();

        props.setEnabled(true);
        props.setPath("/custom-path");
        props.setTitle("Custom Title");
        props.setRefreshInterval(10);
        props.setAuthEnabled(true);

        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getPath()).isEqualTo("/custom-path");
        assertThat(props.getTitle()).isEqualTo("Custom Title");
        assertThat(props.getRefreshInterval()).isEqualTo(10);
        assertThat(props.isAuthEnabled()).isTrue();
    }
}
