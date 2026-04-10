package com.fluffy.batch.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Fluffy Batch metrics exposure.
 */
@ConfigurationProperties(prefix = "fluffy.batch.metrics")
public class MetricsProperties {

    /** Whether metrics are enabled. Defaults to true when actuator is on the classpath. */
    private boolean enabled = true;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
