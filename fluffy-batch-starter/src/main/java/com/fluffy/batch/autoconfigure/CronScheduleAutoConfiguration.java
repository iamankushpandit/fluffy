package com.fluffy.batch.autoconfigure;

import com.fluffy.batch.engine.CronJobScheduler;
import com.fluffy.batch.engine.JobLauncher;
import com.fluffy.batch.engine.JobRegistry;
import com.fluffy.batch.persistence.CronScheduleRepository;
import com.fluffy.batch.web.CronScheduleController;
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
    private CronJobScheduler cronJobSchedulerInstance;

    public CronScheduleAutoConfiguration(CronScheduleProperties properties,
                                          JobLauncher jobLauncher,
                                          JobRegistry jobRegistry,
                                          CronScheduleRepository scheduleRepository) {
        this.properties = properties;
        this.jobLauncher = jobLauncher;
        this.jobRegistry = jobRegistry;
        this.scheduleRepository = scheduleRepository;
    }

    @Bean
    public CronJobScheduler cronJobScheduler() {
        CronJobScheduler scheduler = new CronJobScheduler(jobLauncher, jobRegistry, scheduleRepository, properties);
        scheduler.initializeSchedules();
        this.cronJobSchedulerInstance = scheduler;
        return scheduler;
    }

    @Bean
    public CronScheduleController cronScheduleController(CronScheduleRepository repo, CronJobScheduler scheduler) {
        return new CronScheduleController(repo, scheduler);
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // Use the Spring-managed bean instance instead of calling cronJobScheduler() again
        CronJobScheduler scheduler = this.cronJobSchedulerInstance;
        if (scheduler == null) {
            scheduler = cronJobScheduler();
        }
        // Evaluate cron schedules every 60 seconds
        taskRegistrar.addFixedRateTask(scheduler::evaluateSchedules, 60_000L);
    }
}
