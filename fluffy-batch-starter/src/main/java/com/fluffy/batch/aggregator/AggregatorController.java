package com.fluffy.batch.aggregator;

import com.fluffy.batch.api.NodeSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST endpoints consumed by the aggregator React dashboard.
 * <p>
 * This class is NOT annotated with {@code @Controller} or
 * {@code @RestController} because it must not be picked up by component
 * scanning.  Bean registration is handled exclusively by
 * {@link AggregatorAutoConfiguration} (conditional on property).
 */
@RequestMapping("/api/aggregator")
@ResponseBody
public class AggregatorController {

    private final AggregatorService aggregatorService;
    private final NodeDiscoveryService discoveryService;
    private final AggregatorProperties properties;

    public AggregatorController(AggregatorService aggregatorService,
                                NodeDiscoveryService discoveryService,
                                AggregatorProperties properties) {
        this.aggregatorService = aggregatorService;
        this.discoveryService = discoveryService;
        this.properties = properties;
    }

    /** Returns aggregated metrics plus per-node breakdowns. */
    @GetMapping("/summary")
    public AggregatedSummary getAggregatedSummary() {
        return aggregatorService.getAggregatedSummary();
    }

    /** Returns per-node summaries only. */
    @GetMapping("/nodes")
    public List<NodeSummary> getNodeSummaries() {
        return aggregatorService.getNodeSummaries();
    }

    /** Triggers an immediate re-poll of all nodes. */
    @GetMapping("/refresh")
    public AggregatedSummary refreshAndGet() {
        discoveryService.refresh();
        aggregatorService.pollNodes();
        return aggregatorService.getAggregatedSummary();
    }

    /** Returns aggregator configuration (used by the React UI). */
    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("title", properties.getTitle());
        config.put("pollIntervalSeconds", properties.getPollIntervalSeconds());
        config.put("discoveryIntervalSeconds", properties.getDiscoveryIntervalSeconds());
        config.put("nodeCount", discoveryService.getNodes().size());
        Map<String, String> extUrls = properties.resolveExternalBaseUrls();
        if (!extUrls.isEmpty()) {
            config.put("externalBaseUrls", extUrls);
        }
        return config;
    }
}
