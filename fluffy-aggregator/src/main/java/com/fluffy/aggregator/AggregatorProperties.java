package com.fluffy.aggregator;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for the Fluffy Aggregator service.
 *
 * <pre>
 * fluffy:
 *   aggregator:
 *     nodes:
 *       - http://node1:8080
 *       - http://node2:8080
 *     poll-interval-seconds: 10
 *     discovery-interval-seconds: 30
 *     dashboard-path: /fluffy-aggregator
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "fluffy.aggregator")
public class AggregatorProperties {

    /** Title displayed in the aggregator dashboard UI. */
    private String title = "Fluffy Aggregator Dashboard";

    /** Static list of Fluffy node base URLs (e.g. http://node1:8080). */
    private List<String> nodes = new ArrayList<>();

    /** Interval in seconds between node discovery refreshes. */
    private int discoveryIntervalSeconds = 30;

    /** Interval in seconds between polling node summaries. */
    private int pollIntervalSeconds = 10;

    /** URL path for the aggregator React dashboard UI. */
    private String dashboardPath = "/fluffy-aggregator";

    /** Map of internal node base URL to external base URL for browser link rewriting. */
    private Map<String, String> externalBaseUrls = new LinkedHashMap<>();

    /**
     * Comma-separated list of {@code internal=external} URL pairs for dashboard link
     * rewriting. Example:
     * {@code http://svc1:8080=http://localhost:8080,http://svc2:8080=http://localhost:8081}
     */
    private String externalUrlMappings = "";

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<String> getNodes() { return nodes; }
    public void setNodes(List<String> nodes) { this.nodes = nodes; }

    public int getDiscoveryIntervalSeconds() { return discoveryIntervalSeconds; }
    public void setDiscoveryIntervalSeconds(int v) { this.discoveryIntervalSeconds = v; }

    public int getPollIntervalSeconds() { return pollIntervalSeconds; }
    public void setPollIntervalSeconds(int v) { this.pollIntervalSeconds = v; }

    public String getDashboardPath() { return dashboardPath; }
    public void setDashboardPath(String dashboardPath) { this.dashboardPath = dashboardPath; }

    public Map<String, String> getExternalBaseUrls() { return externalBaseUrls; }
    public void setExternalBaseUrls(Map<String, String> externalBaseUrls) {
        this.externalBaseUrls = externalBaseUrls;
    }

    public String getExternalUrlMappings() { return externalUrlMappings; }
    public void setExternalUrlMappings(String externalUrlMappings) {
        this.externalUrlMappings = externalUrlMappings;
    }

    /**
     * Returns the effective external base URL map, merging the structured map
     * and the comma-separated string for convenience.
     */
    public Map<String, String> resolveExternalBaseUrls() {
        Map<String, String> result = new LinkedHashMap<>(externalBaseUrls);
        if (externalUrlMappings != null && !externalUrlMappings.isBlank()) {
            for (String pair : externalUrlMappings.split(",")) {
                String[] parts = pair.trim().split("=", 2);
                if (parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
                    result.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        return result;
    }
}
