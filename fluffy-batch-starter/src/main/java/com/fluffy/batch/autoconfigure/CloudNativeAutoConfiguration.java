package com.fluffy.batch.autoconfigure;

import com.fluffy.batch.engine.CloudNativeExecutionStrategy;
import com.fluffy.batch.engine.ExecutionCallback;
import com.fluffy.batch.engine.LocalExecutionStrategy;
import com.fluffy.batch.web.CloudNativeCallbackController;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Auto-configuration for execution strategies.
 *
 * <p>Always registers the {@link LocalExecutionStrategy} (default, in-process execution).</p>
 *
 * <p>When {@code fluffy.batch.cloud-native.enabled=true}, additionally registers:
 * <ul>
 *   <li>{@link CloudNativeExecutionStrategy} — dispatches jobs to external orchestrators</li>
 *   <li>{@link CloudNativeCallbackController} — receives status callbacks from orchestrators</li>
 * </ul>
 */
@AutoConfiguration(after = BatchJobAutoConfiguration.class)
@EnableConfigurationProperties(CloudNativeProperties.class)
@Import(CloudNativeCallbackController.class)
public class CloudNativeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LocalExecutionStrategy.class)
    public LocalExecutionStrategy localExecutionStrategy(
            @Qualifier("jobExecutorService") ExecutorService executorService,
            @Qualifier("jobScheduledExecutorService") ScheduledExecutorService scheduledExecutorService,
            ExecutionCallback callback) {
        return new LocalExecutionStrategy(executorService, scheduledExecutorService, callback);
    }

    @Bean
    @ConditionalOnProperty(prefix = "fluffy.batch.cloud-native", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(CloudNativeExecutionStrategy.class)
    public CloudNativeExecutionStrategy cloudNativeExecutionStrategy(
            CloudNativeProperties properties,
            ExecutionCallback callback) {
        return new CloudNativeExecutionStrategy(properties, callback);
    }
}
