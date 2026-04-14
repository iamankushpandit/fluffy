package com.fluffy.aggregator;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AggregatorPropertiesTest {

    @Test
    void shouldHaveDefaults() {
        AggregatorProperties props = new AggregatorProperties();

        assertThat(props.getTitle()).isEqualTo("Fluffy Aggregator Dashboard");
        assertThat(props.getNodes()).isEmpty();
        assertThat(props.getDiscoveryIntervalSeconds()).isEqualTo(30);
        assertThat(props.getPollIntervalSeconds()).isEqualTo(10);
        assertThat(props.getDashboardPath()).isEqualTo("/fluffy-aggregator");
    }

    @Test
    void shouldSetAndGetAllProperties() {
        AggregatorProperties props = new AggregatorProperties();

        props.setTitle("My Aggregator");
        props.setNodes(List.of("http://node1:8080", "http://node2:8080"));
        props.setDiscoveryIntervalSeconds(60);
        props.setPollIntervalSeconds(5);
        props.setDashboardPath("/custom-agg");

        assertThat(props.getTitle()).isEqualTo("My Aggregator");
        assertThat(props.getNodes()).containsExactly("http://node1:8080", "http://node2:8080");
        assertThat(props.getDiscoveryIntervalSeconds()).isEqualTo(60);
        assertThat(props.getPollIntervalSeconds()).isEqualTo(5);
        assertThat(props.getDashboardPath()).isEqualTo("/custom-agg");
    }

    @Test
    void shouldResolveExternalBaseUrlsFromStructuredMap() {
        AggregatorProperties props = new AggregatorProperties();
        props.setExternalBaseUrls(Map.of("http://svc:8080", "http://localhost:8080"));

        Map<String, String> resolved = props.resolveExternalBaseUrls();

        assertThat(resolved).containsEntry("http://svc:8080", "http://localhost:8080");
    }

    @Test
    void shouldResolveExternalBaseUrlsFromCommaSeparatedString() {
        AggregatorProperties props = new AggregatorProperties();
        props.setExternalUrlMappings(
                "http://svc1:8080=http://localhost:8080,http://svc2:8080=http://localhost:8081");

        Map<String, String> resolved = props.resolveExternalBaseUrls();

        assertThat(resolved)
                .containsEntry("http://svc1:8080", "http://localhost:8080")
                .containsEntry("http://svc2:8080", "http://localhost:8081");
    }

    @Test
    void shouldMergeBothExternalUrlSources() {
        AggregatorProperties props = new AggregatorProperties();
        props.setExternalBaseUrls(Map.of("http://svc1:8080", "http://localhost:8080"));
        props.setExternalUrlMappings("http://svc2:8080=http://localhost:8081");

        Map<String, String> resolved = props.resolveExternalBaseUrls();

        assertThat(resolved).hasSize(2);
    }

    @Test
    void shouldReturnEmptyMapWhenNoExternalUrlsConfigured() {
        AggregatorProperties props = new AggregatorProperties();

        assertThat(props.resolveExternalBaseUrls()).isEmpty();
    }

    @Test
    void shouldIgnoreMalformedMappingPairs() {
        AggregatorProperties props = new AggregatorProperties();
        props.setExternalUrlMappings("bad-entry,http://svc:8080=http://localhost:8080");

        Map<String, String> resolved = props.resolveExternalBaseUrls();

        assertThat(resolved).containsOnlyKeys("http://svc:8080");
    }
}
