package com.fluffy.batch.autoconfigure;

import com.fluffy.batch.engine.CronJobScheduler;
import com.fluffy.batch.engine.JobLauncher;
import com.fluffy.batch.engine.JobRegistry;
import com.fluffy.batch.persistence.CronScheduleRepository;
import com.fluffy.batch.web.CronScheduleController;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Auto-configuration for cron-based job scheduling.
 * <p>
 * Activated when {@code fluffy.batch.cron.enabled=true}.
 * Registers periodic evaluation of cron schedules and the REST API
 * for managing schedules.
 */
@AutoConfiguration
@ConditionalOnProperty(name = "fluffy.batch.cron.enabled", havingValue = "true")
@EnableConfigurationProperties(CronScheduleProperties.class)
@EnableScheduling
public class CronScheduleAutoConfiguration implements SchedulingConfigurer {

    private final CronScheduleProperties properties;
    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;
    private final CronScheduleRepository scheduleRepository;
    private final ObjectProvider<CronJobScheduler> cronJobSchedulerProvider;

    public CronScheduleAutoConfiguration(CronScheduleProperties properties,
                                          JobLauncher jobLauncher,
                                          JobRegistry jobRegistry,
                                          CronScheduleRepository scheduleRepository,
                                          ObjectProvider<CronJobScheduler> cronJobSchedulerProvider) {
        this.properties = properties;
        this.jobLauncher = jobLauncher;
        this.jobRegistry = jobRegistry;
        this.scheduleRepository = scheduleRepository;
        this.cronJobSchedulerProvider = cronJobSchedulerProvider;
    }

    @Bean
    public CronJobScheduler cronJobScheduler() {
        CronJobScheduler scheduler = new CronJobScheduler(jobLauncher, jobRegistry, scheduleRepository, properties);
        scheduler.initializeSchedules();
        return scheduler;
    }

    @Bean
    public CronScheduleController cronScheduleController(CronScheduleRepository repo, CronJobScheduler scheduler) {
        return new CronScheduleController(repo, scheduler);
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // Use ObjectProvider to get the Spring-managed bean (avoids duplicate
        // instance since @AutoConfiguration uses proxyBeanMethods=false)
        CronJobScheduler scheduler = cronJobSchedulerProvider.getObject();
        // Evaluate cron schedules every 60 seconds
        taskRegistrar.addFixedRateTask(scheduler::evaluateSchedules, 60_000L);
    }
}
