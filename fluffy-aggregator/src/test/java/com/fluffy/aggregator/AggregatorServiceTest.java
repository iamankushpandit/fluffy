package com.fluffy.aggregator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AggregatorServiceTest {

    private NodeDiscoveryService discoveryService;
    private RestClient restClient;
    private RestClient.ResponseSpec responseSpec;
    private AggregatorService service;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @BeforeEach
    void setUp() {
        discoveryService = mock(NodeDiscoveryService.class);
        restClient = mock(RestClient.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        RestClient.RequestHeadersUriSpec requestSpec = mock(RestClient.RequestHeadersUriSpec.class);
        when(restClient.get()).thenReturn(requestSpec);
        when(requestSpec.uri(anyString())).thenReturn(requestSpec);
        when(requestSpec.retrieve()).thenReturn(responseSpec);

        service = new AggregatorService(discoveryService, restClient);
    }

    @Test
    void shouldPollNodesAndCollectSummaries() {
        NodeSummary summary = new NodeSummary("node-1", 2, 3, 10, 1, true, "http://node1/dash");
        when(discoveryService.getNodes()).thenReturn(List.of("http://node1:8080"));
        when(responseSpec.body(NodeSummary.class)).thenReturn(summary);

        service.pollNodes();

        assertThat(service.getNodeSummaries()).containsExactly(summary);
    }

    @Test
    void shouldReturnEmptyWhenNoNodes() {
        when(discoveryService.getNodes()).thenReturn(List.of());

        service.pollNodes();

        assertThat(service.getNodeSummaries()).isEmpty();
    }

    @Test
    void shouldHandleNodePollFailureGracefully() {
        when(discoveryService.getNodes()).thenReturn(List.of("http://dead-node:8080"));
        when(responseSpec.body(NodeSummary.class)).thenThrow(new RuntimeException("Connection refused"));

        service.pollNodes();

        assertThat(service.getNodeSummaries()).isEmpty();
    }

    @Test
    void shouldReturnAggregatedSummary() {
        NodeSummary n1 = new NodeSummary("n1", 1, 2, 3, 4, false, null);
        NodeSummary n2 = new NodeSummary("n2", 5, 6, 7, 8, true, "http://n2/dash");

        when(discoveryService.getNodes()).thenReturn(List.of("http://n1", "http://n2"));
        when(responseSpec.body(NodeSummary.class)).thenReturn(n1, n2);

        service.pollNodes();

        AggregatedSummary agg = service.getAggregatedSummary();
        assertThat(agg.totalQueued()).isEqualTo(6);
        assertThat(agg.totalRunning()).isEqualTo(8);
        assertThat(agg.totalSucceeded()).isEqualTo(10);
        assertThat(agg.totalFailed()).isEqualTo(12);
        assertThat(agg.nodeSummaries()).hasSize(2);
    }

    @Test
    void shouldSkipNullResponseFromNode() {
        when(discoveryService.getNodes()).thenReturn(List.of("http://node1:8080"));
        when(responseSpec.body(NodeSummary.class)).thenReturn(null);

        service.pollNodes();

        assertThat(service.getNodeSummaries()).isEmpty();
    }

    @Test
    void shouldReplaceOldSummariesOnRePoll() {
        NodeSummary first  = new NodeSummary("n1", 1, 0, 0, 0, false, null);
        NodeSummary second = new NodeSummary("n1", 0, 1, 0, 0, false, null);

        when(discoveryService.getNodes()).thenReturn(List.of("http://n1"));
        when(responseSpec.body(NodeSummary.class)).thenReturn(first);
        service.pollNodes();
        assertThat(service.getNodeSummaries()).containsExactly(first);

        when(responseSpec.body(NodeSummary.class)).thenReturn(second);
        service.pollNodes();
        assertThat(service.getNodeSummaries()).containsExactly(second);
    }
}
