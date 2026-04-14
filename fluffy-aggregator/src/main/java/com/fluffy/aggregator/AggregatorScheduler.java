package com.fluffy.aggregator;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Drives periodic node discovery refreshes and job summary polling.
 */
@Component
public class AggregatorScheduler {

    private final AggregatorService aggregatorService;
    private final NodeDiscoveryService discoveryService;

    public AggregatorScheduler(AggregatorService aggregatorService,
                               NodeDiscoveryService discoveryService) {
        this.aggregatorService = aggregatorService;
        this.discoveryService = discoveryService;
    }

    @Scheduled(fixedDelayString = "${fluffy.aggregator.poll-interval-seconds:10}000")
    public void poll() {
        aggregatorService.pollNodes();
    }

    @Scheduled(fixedDelayString = "${fluffy.aggregator.discovery-interval-seconds:30}000")
    public void discover() {
        discoveryService.refresh();
    }
}
