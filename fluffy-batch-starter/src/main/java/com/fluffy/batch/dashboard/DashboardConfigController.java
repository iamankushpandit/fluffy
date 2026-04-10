package com.fluffy.batch.dashboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class DashboardConfigController {

    private final DashboardProperties properties;

    public DashboardConfigController(DashboardProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/api/jobs/dashboard/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("title", properties.getTitle());
        config.put("refreshInterval", properties.getRefreshInterval());
        config.put("authEnabled", properties.isAuthEnabled());
        return config;
    }
}
