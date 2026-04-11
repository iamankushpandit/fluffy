package com.fluffy.batch.aggregator;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for the multi-node aggregator layer.
 * <p>
 * Set {@code fluffy.batch.aggregator.enabled=true} to activate the aggregator.
 * Provide node base URLs via {@code fluffy.batch.aggregator.nodes}.
 */
@ConfigurationProperties(prefix = "fluffy.batch.aggregator")
public class AggregatorProperties {

    /** Whether the aggregator feature is active. */
    private boolean enabled = false;

    /** Static list of node base URLs (e.g. http://node1:8080). */
    private List<String> nodes = new ArrayList<>();

    /** Interval in seconds between node discovery refreshes. */
    private int discoveryIntervalSeconds = 30;

    /** Interval in seconds between polling node summaries. */
    private int pollIntervalSeconds = 10;

    /** Path to the aggregator React dashboard UI. */
    private String dashboardPath = "/fluffy-aggregator";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public List<String> getNodes() { return nodes; }
    public void setNodes(List<String> nodes) { this.nodes = nodes; }

    public int getDiscoveryIntervalSeconds() { return discoveryIntervalSeconds; }
    public void setDiscoveryIntervalSeconds(int discoveryIntervalSeconds) {
        this.discoveryIntervalSeconds = discoveryIntervalSeconds;
    }

    public int getPollIntervalSeconds() { return pollIntervalSeconds; }
    public void setPollIntervalSeconds(int pollIntervalSeconds) {
        this.pollIntervalSeconds = pollIntervalSeconds;
    }

    public String getDashboardPath() { return dashboardPath; }
    public void setDashboardPath(String dashboardPath) { this.dashboardPath = dashboardPath; }
}
