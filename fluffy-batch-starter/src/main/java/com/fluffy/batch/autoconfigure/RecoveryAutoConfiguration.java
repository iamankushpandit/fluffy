package com.fluffy.batch.autoconfigure;

import com.fluffy.batch.engine.NodeIdResolver;
import com.fluffy.batch.engine.RecoveryManager;
import com.fluffy.batch.persistence.JobExecutionRepository;
import com.fluffy.batch.persistence.NodeHeartbeatRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Auto-configuration for node heartbeat and recovery.
 * <p>
 * Activated when {@code fluffy.batch.recovery.enabled=true}.
 * Registers periodic heartbeat and stale-node detection tasks.
 * </p>
 */
@AutoConfiguration
@ConditionalOnProperty(name = "fluffy.batch.recovery.enabled", havingValue = "true")
@EnableConfigurationProperties(RecoveryProperties.class)
@EnableScheduling
public class RecoveryAutoConfiguration implements SchedulingConfigurer {

    private final RecoveryProperties properties;
    private final NodeHeartbeatRepository heartbeatRepository;
    private final JobExecutionRepository executionRepository;

    public RecoveryAutoConfiguration(RecoveryProperties properties,
                                     NodeHeartbeatRepository heartbeatRepository,
                                     JobExecutionRepository executionRepository) {
        this.properties = properties;
        this.heartbeatRepository = heartbeatRepository;
        this.executionRepository = executionRepository;
    }

    @Bean
    public RecoveryManager recoveryManager() {
        return new RecoveryManager(NodeIdResolver.getNodeId(), properties, heartbeatRepository, executionRepository);
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        RecoveryManager manager = recoveryManager();
        long heartbeatMs = properties.getHeartbeatInterval() * 1000L;
        long recoveryMs = properties.getStaleThreshold() * 1000L;

        taskRegistrar.addFixedRateTask(manager::sendHeartbeat, heartbeatMs);
        taskRegistrar.addFixedRateTask(manager::recoverStaleNodes, recoveryMs);
    }
}
