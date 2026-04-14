package com.fluffy.batch.engine;

import com.fluffy.batch.autoconfigure.CronScheduleProperties;
import com.fluffy.batch.model.CronSchedule;
import com.fluffy.batch.persistence.CronScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CronJobSchedulerTest {

    private JobLauncher jobLauncher;
    private JobRegistry jobRegistry;
    private CronScheduleRepository scheduleRepository;
    private CronScheduleProperties properties;
    private CronJobScheduler scheduler;

    @BeforeEach
    void setUp() {
        jobLauncher = mock(JobLauncher.class);
        jobRegistry = mock(JobRegistry.class);
        scheduleRepository = mock(CronScheduleRepository.class);
        properties = new CronScheduleProperties();
        scheduler = new CronJobScheduler(jobLauncher, jobRegistry, scheduleRepository, properties);
    }

    @Test
    void shouldHaveNonBlankNodeId() {
        assertThat(scheduler.getNodeId()).isNotBlank();
    }

    @Test
    void shouldInitializeSchedulesFromAnnotations() {
        JobDefinition def = JobDefinition.builder("cron-job")
                .cronExpression("0 0 * * * *")
                .handler(ctx -> {})
                .build();
        when(jobRegistry.getAll()).thenReturn(List.of(def));
        when(scheduleRepository.findByJobName("cron-job")).thenReturn(Optional.empty());

        scheduler.initializeSchedules();

        verify(scheduleRepository).save(argThat(s -> s.getJobName().equals("cron-job")));
    }

    @Test
    void shouldInitializeSchedulesFromProperties() {
        when(jobRegistry.getAll()).thenReturn(List.of());
        properties.setSchedules(Map.of("prop-job", "0 30 2 * * *"));
        when(scheduleRepository.findByJobName("prop-job")).thenReturn(Optional.empty());

        scheduler.initializeSchedules();

        verify(scheduleRepository).save(argThat(s -> s.getJobName().equals("prop-job")));
    }

    @Test
    void shouldNotDuplicateExistingSchedules() {
        JobDefinition def = JobDefinition.builder("existing-job")
                .cronExpression("0 0 * * * *")
                .handler(ctx -> {})
                .build();
        when(jobRegistry.getAll()).thenReturn(List.of(def));
        when(scheduleRepository.findByJobName("existing-job"))
                .thenReturn(Optional.of(new CronSchedule("existing-job", "0 0 * * * *")));

        scheduler.initializeSchedules();

        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void shouldSkipJobsWithBlankCronExpression() {
        JobDefinition def = JobDefinition.builder("no-cron-job")
                .handler(ctx -> {})
                .build();
        when(jobRegistry.getAll()).thenReturn(List.of(def));

        scheduler.initializeSchedules();

        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void shouldDetectDueScheduleFirstTime() {
        CronSchedule schedule = new CronSchedule("test-job", "0 0 * * * *");
        // lastTriggeredAt is null — should be due immediately
        assertThat(scheduler.isDue(schedule, Instant.now())).isTrue();
    }

    @Test
    void shouldDetectDueScheduleAfterCronInterval() {
        CronSchedule schedule = new CronSchedule("test-job", "0 0 * * * *");
        // Last triggered 2 hours ago — next trigger should have passed
        schedule.setLastTriggeredAt(Instant.now().minus(2, ChronoUnit.HOURS));
        assertThat(scheduler.isDue(schedule, Instant.now())).isTrue();
    }

    @Test
    void shouldNotBeDueIfRecentlyTriggered() {
        CronSchedule schedule = new CronSchedule("test-job", "0 0 * * * *");
        // Last triggered just now — next trigger is in ~1 hour
        schedule.setLastTriggeredAt(Instant.now());
        assertThat(scheduler.isDue(schedule, Instant.now())).isFalse();
    }

    @Test
    void shouldEvaluateAndLaunchDueSchedules() {
        CronSchedule schedule = new CronSchedule("my-job", "0 0 * * * *");
        // Never triggered
        when(scheduleRepository.findByEnabled(true)).thenReturn(List.of(schedule));
        when(jobRegistry.exists("my-job")).thenReturn(true);
        when(jobLauncher.launch(eq("my-job"), any())).thenReturn(1L);

        scheduler.evaluateSchedules();

        verify(jobLauncher).launch(eq("my-job"), any());
        verify(scheduleRepository).save(argThat(s -> s.getLastTriggeredAt() != null));
    }

    @Test
    void shouldSkipSchedulesForDifferentNode() {
        CronSchedule schedule = new CronSchedule("targeted-job", "0 0 * * * *");
        schedule.setTargetNode("other-node-that-doesnt-match");
        when(scheduleRepository.findByEnabled(true)).thenReturn(List.of(schedule));

        scheduler.evaluateSchedules();

        verify(jobLauncher, never()).launch(any(), any());
    }

    @Test
    void shouldSkipUnregisteredJobs() {
        CronSchedule schedule = new CronSchedule("unknown-job", "0 0 * * * *");
        when(scheduleRepository.findByEnabled(true)).thenReturn(List.of(schedule));
        when(jobRegistry.exists("unknown-job")).thenReturn(false);

        scheduler.evaluateSchedules();

        verify(jobLauncher, never()).launch(any(), any());
    }

    @Test
    void shouldGetUpcomingTriggers() {
        CronSchedule schedule = new CronSchedule("hourly-job", "0 0 * * * *");
        schedule.setEnabled(true);
        when(scheduleRepository.findByEnabled(true)).thenReturn(List.of(schedule));

        List<Map<String, Object>> upcoming = scheduler.getUpcomingTriggers(3);

        assertThat(upcoming).hasSize(1);
        assertThat(upcoming.get(0)).containsEntry("jobName", "hourly-job");
        @SuppressWarnings("unchecked")
        List<String> times = (List<String>) upcoming.get(0).get("upcoming");
        assertThat(times).hasSize(3);
    }

    @Test
    void shouldGetAllSchedules() {
        CronSchedule s1 = new CronSchedule("job1", "0 0 * * * *");
        CronSchedule s2 = new CronSchedule("job2", "0 30 2 * * *");
        when(scheduleRepository.findAll()).thenReturn(List.of(s1, s2));

        List<CronSchedule> result = scheduler.getAllSchedules();

        assertThat(result).hasSize(2);
    }

    @Test
    void shouldHandleExceptionDuringEvaluation() {
        CronSchedule schedule = new CronSchedule("error-job", "0 0 * * * *");
        when(scheduleRepository.findByEnabled(true)).thenReturn(List.of(schedule));
        when(jobRegistry.exists("error-job")).thenReturn(true);
        when(jobLauncher.launch(eq("error-job"), any())).thenThrow(new RuntimeException("boom"));

        // Should not throw
        assertThatCode(() -> scheduler.evaluateSchedules()).doesNotThrowAnyException();
    }
}
