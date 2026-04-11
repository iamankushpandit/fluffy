package com.fluffy.batch.aggregator;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AggregatorPropertiesTest {

    @Test
    void shouldHaveDefaults() {
        AggregatorProperties props = new AggregatorProperties();

        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getNodes()).isEmpty();
        assertThat(props.getDiscoveryIntervalSeconds()).isEqualTo(30);
        assertThat(props.getPollIntervalSeconds()).isEqualTo(10);
        assertThat(props.getDashboardPath()).isEqualTo("/fluffy-aggregator");
    }

    @Test
    void shouldSetAndGetAllProperties() {
        AggregatorProperties props = new AggregatorProperties();

        props.setEnabled(true);
        props.setNodes(List.of("http://node1:8080", "http://node2:8080"));
        props.setDiscoveryIntervalSeconds(60);
        props.setPollIntervalSeconds(5);
        props.setDashboardPath("/custom-agg");

        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getNodes()).containsExactly("http://node1:8080", "http://node2:8080");
        assertThat(props.getDiscoveryIntervalSeconds()).isEqualTo(60);
        assertThat(props.getPollIntervalSeconds()).isEqualTo(5);
        assertThat(props.getDashboardPath()).isEqualTo("/custom-agg");
    }
}
