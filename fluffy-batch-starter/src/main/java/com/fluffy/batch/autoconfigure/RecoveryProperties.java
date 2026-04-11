package com.fluffy.batch.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for fault-tolerant node recovery.
 * <p>
 * Recovery is only active for database-backed and Kafka-backed modes.
 * In H2 mode, recovery is automatically disabled.
 * </p>
 */
@ConfigurationProperties(prefix = "fluffy.batch.recovery")
public class RecoveryProperties {

    /** Whether node recovery is enabled. Defaults to false. */
    private boolean enabled = false;

    /** Interval in seconds between heartbeat updates. Defaults to 15 seconds. */
    private int heartbeatInterval = 15;

    /** Threshold in seconds after which a node is considered stale. Defaults to 60 seconds. */
    private int staleThreshold = 60;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getHeartbeatInterval() { return heartbeatInterval; }
    public void setHeartbeatInterval(int heartbeatInterval) { this.heartbeatInterval = heartbeatInterval; }

    public int getStaleThreshold() { return staleThreshold; }
    public void setStaleThreshold(int staleThreshold) { this.staleThreshold = staleThreshold; }
}
