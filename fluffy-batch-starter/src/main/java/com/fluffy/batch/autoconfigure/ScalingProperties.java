package com.fluffy.batch.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for autoscaling-friendly behavior.
 */
@ConfigurationProperties(prefix = "fluffy.batch.scaling")
public class ScalingProperties {

    /** Maximum queue depth threshold — used as signal for external autoscalers. */
    private int maxQueueDepth = 100;

    /** Whether runtime updates to scaling configuration are allowed via API. */
    private boolean runtimeUpdatesEnabled = true;

    public int getMaxQueueDepth() { return maxQueueDepth; }
    public void setMaxQueueDepth(int maxQueueDepth) { this.maxQueueDepth = maxQueueDepth; }

    public boolean isRuntimeUpdatesEnabled() { return runtimeUpdatesEnabled; }
    public void setRuntimeUpdatesEnabled(boolean runtimeUpdatesEnabled) { this.runtimeUpdatesEnabled = runtimeUpdatesEnabled; }
}
