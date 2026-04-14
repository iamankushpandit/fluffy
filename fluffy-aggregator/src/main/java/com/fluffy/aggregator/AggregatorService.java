package com.fluffy.aggregator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Polls every discovered Fluffy node's {@code /api/jobs/summary} endpoint and
 * caches the latest per-node and aggregated results.
 */
@Service
public class AggregatorService {

    private static final Logger log = LoggerFactory.getLogger(AggregatorService.class);

    private final NodeDiscoveryService discoveryService;
    private final RestClient restClient;
    private final List<NodeSummary> latestSummaries = new CopyOnWriteArrayList<>();

    public AggregatorService(NodeDiscoveryService discoveryService, RestClient restClient) {
        this.discoveryService = discoveryService;
        this.restClient = restClient;
    }

    /** Polls all discovered nodes and updates the cached summaries. */
    public void pollNodes() {
        List<String> nodes = discoveryService.getNodes();
        List<NodeSummary> results = new ArrayList<>();
        for (String baseUrl : nodes) {
            try {
                NodeSummary summary = restClient.get()
                        .uri(baseUrl + "/api/jobs/summary")
                        .retrieve()
                        .body(NodeSummary.class);
                if (summary != null) {
                    results.add(summary);
                }
            } catch (Exception e) {
                log.warn("Failed to poll node {}: {}", baseUrl, e.getMessage());
            }
        }
        latestSummaries.clear();
        latestSummaries.addAll(results);
        log.debug("Polled {} node(s), received {} summaries", nodes.size(), results.size());
    }

    /** Returns the most recently collected per-node summaries. */
    public List<NodeSummary> getNodeSummaries() {
        return Collections.unmodifiableList(new ArrayList<>(latestSummaries));
    }

    /** Returns the aggregated summary across all nodes. */
    public AggregatedSummary getAggregatedSummary() {
        return AggregatedSummary.from(getNodeSummaries());
    }
}
