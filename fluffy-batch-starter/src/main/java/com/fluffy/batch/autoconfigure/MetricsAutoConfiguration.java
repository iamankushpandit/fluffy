package com.fluffy.batch.autoconfigure;

import com.fluffy.batch.backend.CoordinationBackend;
import com.fluffy.batch.backend.QueueBackend;
import com.fluffy.batch.engine.FluffyBatchMetrics;
import com.fluffy.batch.persistence.JobExecutionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Fluffy Batch metrics.
 * Activated when Micrometer is on the classpath and metrics are not disabled.
 * Requires a MeterRegistry bean to be available (typically from spring-boot-starter-actuator).
 */
@AutoConfiguration
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean(MeterRegistry.class)
@ConditionalOnProperty(name = "fluffy.batch.metrics.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MetricsProperties.class)
public class MetricsAutoConfiguration {

    @Bean
    public FluffyBatchMetrics fluffyBatchMetrics(MeterRegistry registry,
                                                  QueueBackend queueBackend,
                                                  CoordinationBackend coordinationBackend,
                                                  JobExecutionRepository executionRepository) {
        return new FluffyBatchMetrics(registry, queueBackend, coordinationBackend, executionRepository);
    }
}
