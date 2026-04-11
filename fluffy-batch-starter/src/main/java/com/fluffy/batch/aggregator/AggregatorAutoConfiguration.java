package com.fluffy.batch.aggregator;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Auto-configuration for the multi-node aggregator layer.
 * Activated when {@code fluffy.batch.aggregator.enabled=true}.
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnProperty(name = "fluffy.batch.aggregator.enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(AggregatorProperties.class)
@EnableScheduling
public class AggregatorAutoConfiguration {

    @Bean
    public RestClient aggregatorRestClient() {
        return RestClient.create();
    }

    @Bean
    public NodeDiscoveryService nodeDiscoveryService(AggregatorProperties properties) {
        return new NodeDiscoveryService(properties);
    }

    @Bean
    public AggregatorService aggregatorService(NodeDiscoveryService discoveryService,
                                               RestClient aggregatorRestClient) {
        return new AggregatorService(discoveryService, aggregatorRestClient);
    }

    @Bean
    public AggregatorController aggregatorController(AggregatorService aggregatorService,
                                                     NodeDiscoveryService discoveryService,
                                                     AggregatorProperties properties) {
        return new AggregatorController(aggregatorService, discoveryService, properties);
    }

    @Bean
    public AggregatorScheduler aggregatorScheduler(AggregatorService aggregatorService,
                                                   NodeDiscoveryService discoveryService,
                                                   AggregatorProperties properties) {
        return new AggregatorScheduler(aggregatorService, discoveryService, properties);
    }

    @Bean
    public WebMvcConfigurer aggregatorDashboardConfigurer(AggregatorProperties properties) {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                String path = normalizePath(properties.getDashboardPath());
                registry.addResourceHandler(path + "/**")
                        .addResourceLocations("classpath:/static/fluffy-aggregator/");
            }
        };
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/fluffy-aggregator";
        }
        String normalized = path.startsWith("/") ? path : "/" + path;
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /**
     * Inner bean that drives periodic discovery refresh and node polling.
     */
    public static class AggregatorScheduler {

        private final AggregatorService aggregatorService;
        private final NodeDiscoveryService discoveryService;
        private final AggregatorProperties properties;

        public AggregatorScheduler(AggregatorService aggregatorService,
                                   NodeDiscoveryService discoveryService,
                                   AggregatorProperties properties) {
            this.aggregatorService = aggregatorService;
            this.discoveryService = discoveryService;
            this.properties = properties;
        }

        @Scheduled(fixedDelayString = "${fluffy.batch.aggregator.poll-interval-seconds:10}000")
        public void poll() {
            aggregatorService.pollNodes();
        }

        @Scheduled(fixedDelayString = "${fluffy.batch.aggregator.discovery-interval-seconds:30}000")
        public void discover() {
            discoveryService.refresh();
        }
    }
}
