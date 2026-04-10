package com.fluffy.batch.dashboard;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class DashboardConfigControllerTest {

    @Test
    void shouldReturnConfigFromProperties() {
        DashboardProperties props = new DashboardProperties();
        props.setTitle("My Dashboard");
        props.setRefreshInterval(15);
        props.setAuthEnabled(true);

        DashboardConfigController controller = new DashboardConfigController(props);
        Map<String, Object> config = controller.getConfig();

        assertThat(config).containsEntry("title", "My Dashboard");
        assertThat(config).containsEntry("refreshInterval", 15);
        assertThat(config).containsEntry("authEnabled", true);
    }

    @Test
    void shouldReturnDefaultConfig() {
        DashboardProperties props = new DashboardProperties();
        DashboardConfigController controller = new DashboardConfigController(props);
        Map<String, Object> config = controller.getConfig();

        assertThat(config).containsEntry("title", "Fluffy Batch Dashboard");
        assertThat(config).containsEntry("refreshInterval", 5);
        assertThat(config).containsEntry("authEnabled", false);
    }
}
