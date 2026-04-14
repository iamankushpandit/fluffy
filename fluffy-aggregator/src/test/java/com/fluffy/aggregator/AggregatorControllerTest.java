package com.fluffy.aggregator;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AggregatorControllerTest {

    private final AggregatorService aggregatorService = mock(AggregatorService.class);
    private final NodeDiscoveryService discoveryService = mock(NodeDiscoveryService.class);
    private final AggregatorProperties properties = new AggregatorProperties();

    private final AggregatorController controller =
            new AggregatorController(aggregatorService, discoveryService, properties);

    @Test
    void shouldReturnAggregatedSummary() {
        AggregatedSummary expected = new AggregatedSummary(1, 2, 3, 4, List.of());
        when(aggregatorService.getAggregatedSummary()).thenReturn(expected);

        AggregatedSummary result = controller.getAggregatedSummary();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldReturnNodeSummaries() {
        NodeSummary n = new NodeSummary("n1", 1, 2, 3, 4, true, "http://n1/dash");
        when(aggregatorService.getNodeSummaries()).thenReturn(List.of(n));

        List<NodeSummary> result = controller.getNodeSummaries();

        assertThat(result).containsExactly(n);
    }

    @Test
    void shouldRefreshAndReturnAggregatedSummary() {
        AggregatedSummary expected = new AggregatedSummary(0, 0, 0, 0, List.of());
        when(aggregatorService.getAggregatedSummary()).thenReturn(expected);

        AggregatedSummary result = controller.refreshAndGet();

        verify(discoveryService).refresh();
        verify(aggregatorService).pollNodes();
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldReturnConfig() {
        properties.setPollIntervalSeconds(15);
        properties.setDiscoveryIntervalSeconds(45);
        when(discoveryService.getNodes()).thenReturn(List.of("http://a", "http://b"));

        Map<String, Object> config = controller.getConfig();

        assertThat(config).containsEntry("pollIntervalSeconds", 15);
        assertThat(config).containsEntry("discoveryIntervalSeconds", 45);
        assertThat(config).containsEntry("nodeCount", 2);
    }

    @Test
    void shouldIncludeExternalBaseUrlsInConfigWhenSet() {
        properties.setExternalUrlMappings("http://svc1:8080=http://localhost:8080");
        when(discoveryService.getNodes()).thenReturn(List.of());

        Map<String, Object> config = controller.getConfig();

        assertThat(config).containsKey("externalBaseUrls");
    }

    @Test
    void shouldNotIncludeExternalBaseUrlsInConfigWhenEmpty() {
        when(discoveryService.getNodes()).thenReturn(List.of());

        Map<String, Object> config = controller.getConfig();

        assertThat(config).doesNotContainKey("externalBaseUrls");
    }
}
