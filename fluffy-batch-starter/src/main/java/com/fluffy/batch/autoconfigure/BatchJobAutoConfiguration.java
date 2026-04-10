package com.fluffy.batch.autoconfigure;

import com.fluffy.batch.engine.ConcurrencyManager;
import com.fluffy.batch.engine.JobLauncher;
import com.fluffy.batch.engine.JobRegistry;
import com.fluffy.batch.persistence.JobQueueManager;
import com.fluffy.batch.web.GlobalExceptionHandler;
import com.fluffy.batch.web.JobController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@AutoConfiguration
@ConditionalOnClass(JobRegistry.class)
@EnableJpaRepositories(basePackages = "com.fluffy.batch.persistence")
@EntityScan(basePackages = "com.fluffy.batch.model")
@Import({JobRegistry.class, JobLauncher.class, ConcurrencyManager.class,
         JobQueueManager.class, JobController.class, GlobalExceptionHandler.class})
public class BatchJobAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ExecutorService jobExecutorService() {
        return Executors.newCachedThreadPool();
    }

    @Bean
    @ConditionalOnMissingBean
    public ScheduledExecutorService jobScheduledExecutorService() {
        return Executors.newScheduledThreadPool(4);
    }
}
