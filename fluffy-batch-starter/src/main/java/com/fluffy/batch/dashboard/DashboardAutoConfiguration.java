package com.fluffy.batch.dashboard;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnProperty(name = "fluffy.batch.dashboard.enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(DashboardProperties.class)
public class DashboardAutoConfiguration {

    @Bean
    public DashboardConfigController dashboardConfigController(DashboardProperties properties) {
        return new DashboardConfigController(properties);
    }

    @Bean
    public WebMvcConfigurer dashboardResourceConfigurer(DashboardProperties properties) {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                String path = normalizePath(properties.getPath());
                registry.addResourceHandler(path + "/**")
                        .addResourceLocations("classpath:/static/fluffy-dashboard/");
            }
        };
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/fluffy-dashboard";
        }
        String normalized = path.startsWith("/") ? path : "/" + path;
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
