package com.fluffy.aggregator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Standalone Fluffy Aggregator Service.
 *
 * <p>Polls multiple Fluffy batch nodes via their {@code /api/jobs/summary} endpoints,
 * aggregates the results, and serves a unified React/MUI dashboard at
 * {@code /fluffy-aggregator}.
 *
 * <p>Configure nodes via:
 * <pre>
 * fluffy:
 *   aggregator:
 *     nodes:
 *       - http://node1:8080
 *       - http://node2:8080
 * </pre>
 */
@SpringBootApplication
@EnableScheduling
public class FluffyAggregatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(FluffyAggregatorApplication.class, args);
    }
}
