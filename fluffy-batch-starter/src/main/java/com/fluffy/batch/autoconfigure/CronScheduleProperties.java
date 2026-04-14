package com.fluffy.batch.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration properties for cron-based job scheduling.
 *
 * <pre>
 * fluffy:
 *   batch:
 *     cron:
 *       enabled: true
 *       schedules:
 *         data-sync: "0 0 * * * *"
 *         report-generation: "0 30 2 * * *"
 * </pre>
 */
@ConfigurationProperties(prefix = "fluffy.batch.cron")
public class CronScheduleProperties {

    private boolean enabled = false;

    /** Map of job name → cron expression configured via properties. */
    private Map<String, String> schedules = new LinkedHashMap<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Map<String, String> getSchedules() { return schedules; }
    public void setSchedules(Map<String, String> schedules) { this.schedules = schedules; }
}
