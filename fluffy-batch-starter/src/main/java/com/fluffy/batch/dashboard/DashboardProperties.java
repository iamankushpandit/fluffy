package com.fluffy.batch.dashboard;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fluffy.batch.dashboard")
public class DashboardProperties {

    private boolean enabled = true;
    private String path = "/fluffy-dashboard";
    private String title = "Fluffy Batch Dashboard";
    private int refreshInterval = 5;
    private boolean authEnabled = false;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getRefreshInterval() { return refreshInterval; }
    public void setRefreshInterval(int refreshInterval) { this.refreshInterval = refreshInterval; }

    public boolean isAuthEnabled() { return authEnabled; }
    public void setAuthEnabled(boolean authEnabled) { this.authEnabled = authEnabled; }
}
