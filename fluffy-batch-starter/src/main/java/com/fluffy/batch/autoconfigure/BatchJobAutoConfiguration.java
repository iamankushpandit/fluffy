package com.fluffy.batch.autoconfigure;

import com.fluffy.batch.backend.BackendType;
import com.fluffy.batch.backend.CoordinationBackend;
import com.fluffy.batch.backend.QueueBackend;
import com.fluffy.batch.engine.ConcurrencyManager;
import com.fluffy.batch.engine.JobLauncher;
import com.fluffy.batch.engine.JobRegistry;
import com.fluffy.batch.persistence.JobQueueManager;
import com.fluffy.batch.web.GlobalExceptionHandler;
import com.fluffy.batch.web.JobController;
import com.fluffy.batch.web.NodeSummaryController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@AutoConfiguration(before = BatchAutoConfiguration.class)
@ConditionalOnClass(JobRegistry.class)
@EnableJpaRepositories(basePackages = "com.fluffy.batch.persistence")
@EntityScan(basePackages = "com.fluffy.batch.model")
@EnableConfigurationProperties(BackendProperties.class)
@Import({JobRegistry.class, JobLauncher.class,
         JobController.class, NodeSummaryController.class, GlobalExceptionHandler.class})
public class BatchJobAutoConfiguration {

    /**
     * Creates the appropriate {@link QueueBackend} based on the configured backend type.
     * For H2 mode, uses in-memory queue. Database and Kafka modes will provide
     * their own implementations when those modules are active.
     */
    @Bean
    @ConditionalOnMissingBean(QueueBackend.class)
    public QueueBackend queueBackend(BackendProperties properties) {
        return new JobQueueManager();
    }

    /**
     * Creates the appropriate {@link CoordinationBackend} based on the configured backend type.
     * For H2 mode, uses in-memory coordination. Database and Kafka modes will provide
     * their own implementations when those modules are active.
     */
    @Bean
    @ConditionalOnMissingBean(CoordinationBackend.class)
    public CoordinationBackend coordinationBackend(BackendProperties properties) {
        return new ConcurrencyManager();
    }

    /**
     * Uses Java 21 virtual threads for lightweight, high-throughput job execution.
     * Virtual threads are ideal for batch jobs that spend time waiting on I/O,
     * as they scale to millions of concurrent tasks without exhausting OS threads.
     */
    @Bean(name = "jobExecutorService")
    @ConditionalOnMissingBean(name = "jobExecutorService")
    public ExecutorService jobExecutorService() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean(name = "jobScheduledExecutorService")
    @ConditionalOnMissingBean(name = "jobScheduledExecutorService")
    public ScheduledExecutorService jobScheduledExecutorService() {
        return Executors.newScheduledThreadPool(4);
    }
}

