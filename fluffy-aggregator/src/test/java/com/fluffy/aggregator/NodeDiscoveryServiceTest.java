package com.fluffy.aggregator;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NodeDiscoveryServiceTest {

    @Test
    void shouldDiscoverConfiguredNodes() {
        AggregatorProperties props = new AggregatorProperties();
        props.setNodes(List.of("http://node1:8080", "http://node2:8080"));

        NodeDiscoveryService service = new NodeDiscoveryService(props);

        assertThat(service.getNodes()).containsExactly("http://node1:8080", "http://node2:8080");
    }

    @Test
    void shouldReturnEmptyWhenNoNodesConfigured() {
        AggregatorProperties props = new AggregatorProperties();

        NodeDiscoveryService service = new NodeDiscoveryService(props);

        assertThat(service.getNodes()).isEmpty();
    }

    @Test
    void shouldNormalizeTrailingSlashes() {
        AggregatorProperties props = new AggregatorProperties();
        props.setNodes(List.of("http://node1:8080/", "http://node2:8080//"));

        NodeDiscoveryService service = new NodeDiscoveryService(props);

        assertThat(service.getNodes()).containsExactly("http://node1:8080", "http://node2:8080");
    }

    @Test
    void shouldSkipBlankEntries() {
        AggregatorProperties props = new AggregatorProperties();
        props.setNodes(List.of("http://node1:8080", "  ", "http://node2:8080"));

        NodeDiscoveryService service = new NodeDiscoveryService(props);

        assertThat(service.getNodes()).containsExactly("http://node1:8080", "http://node2:8080");
    }

    @Test
    void shouldTrimWhitespace() {
        AggregatorProperties props = new AggregatorProperties();
        props.setNodes(List.of("  http://node1:8080  "));

        NodeDiscoveryService service = new NodeDiscoveryService(props);

        assertThat(service.getNodes()).containsExactly("http://node1:8080");
    }

    @Test
    void shouldRefreshNodeList() {
        AggregatorProperties props = new AggregatorProperties();
        props.setNodes(List.of("http://node1:8080"));

        NodeDiscoveryService service = new NodeDiscoveryService(props);
        assertThat(service.getNodes()).hasSize(1);

        props.setNodes(List.of("http://node1:8080", "http://node2:8080"));
        service.refresh();

        assertThat(service.getNodes()).hasSize(2);
    }

    @Test
    void shouldReturnUnmodifiableList() {
        AggregatorProperties props = new AggregatorProperties();
        props.setNodes(List.of("http://node1:8080"));

        NodeDiscoveryService service = new NodeDiscoveryService(props);

        assertThat(service.getNodes()).isUnmodifiable();
    }

    @Test
    void shouldHandleNullNodeList() {
        AggregatorProperties props = new AggregatorProperties();
        props.setNodes(null);

        NodeDiscoveryService service = new NodeDiscoveryService(props);

        assertThat(service.getNodes()).isEmpty();
    }
}
