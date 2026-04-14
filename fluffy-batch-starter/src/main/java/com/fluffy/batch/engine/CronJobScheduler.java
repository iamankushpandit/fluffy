package com.fluffy.batch.engine;

import com.fluffy.batch.api.JobRequest;
import com.fluffy.batch.autoconfigure.CronScheduleProperties;
import com.fluffy.batch.model.CronSchedule;
import com.fluffy.batch.persistence.CronScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronExpression;

import java.net.InetAddress;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Manages cron-based job scheduling. Merges schedules from three sources:
 * <ol>
 *   <li>{@code @BatchJob(cronExpression = "...")} annotation on the handler</li>
 *   <li>Property-based schedules in {@code fluffy.batch.cron.schedules}</li>
 *   <li>Database-stored schedules created via REST API</li>
 * </ol>
 * <p>
 * When a schedule's cron expression fires, this class delegates to
 * {@link JobLauncher#launch(String, JobRequest)} to start the job.
 */
public class CronJobScheduler {

    private static final Logger log = LoggerFactory.getLogger(CronJobScheduler.class);

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;
    private final CronScheduleRepository scheduleRepository;
    private final CronScheduleProperties properties;
    private final String nodeId;

    public CronJobScheduler(JobLauncher jobLauncher,
                            JobRegistry jobRegistry,
                            CronScheduleRepository scheduleRepository,
                            CronScheduleProperties properties) {
        this.jobLauncher = jobLauncher;
        this.jobRegistry = jobRegistry;
        this.scheduleRepository = scheduleRepository;
        this.properties = properties;
        this.nodeId = resolveNodeId();
    }

    /**
     * Initializes schedules from annotations and properties into the database
     * (if not already present).
     */
    public void initializeSchedules() {
        // From annotations
        for (JobDefinition def : jobRegistry.getAll()) {
            if (def.cronExpression() != null && !def.cronExpression().isBlank()) {
                saveIfAbsent(def.name(), def.cronExpression());
            }
        }
        // From properties
        if (properties.getSchedules() != null) {
            properties.getSchedules().forEach(this::saveIfAbsent);
        }
    }

    private void saveIfAbsent(String jobName, String cronExpression) {
        Optional<CronSchedule> existing = scheduleRepository.findByJobName(jobName);
        if (existing.isEmpty()) {
            CronSchedule schedule = new CronSchedule(jobName, cronExpression);
            scheduleRepository.save(schedule);
            log.info("Registered cron schedule for job '{}': {}", jobName, cronExpression);
        }
    }

    /**
     * Called periodically by the auto-configuration scheduling. Checks each
     * enabled schedule and fires the job if due.
     */
    public void evaluateSchedules() {
        List<CronSchedule> schedules = scheduleRepository.findByEnabled(true);
        Instant now = Instant.now();

        for (CronSchedule schedule : schedules) {
            try {
                // Skip if targeted at a different node
                if (schedule.getTargetNode() != null && !schedule.getTargetNode().isBlank()
                        && !schedule.getTargetNode().equals(nodeId)) {
                    continue;
                }

                if (!jobRegistry.exists(schedule.getJobName())) {
                    log.debug("Skipping schedule for unregistered job '{}'", schedule.getJobName());
                    continue;
                }

                if (!isDue(schedule, now)) {
                    continue;
                }

                log.info("Cron trigger firing for job '{}' (cron={})", schedule.getJobName(), schedule.getCronExpression());
                JobRequest request = new JobRequest(null, null, "cron-scheduler");
                jobLauncher.launch(schedule.getJobName(), request);

                schedule.setLastTriggeredAt(now);
                scheduleRepository.save(schedule);
            } catch (Exception e) {
                log.error("Failed to evaluate cron schedule for job '{}': {}",
                        schedule.getJobName(), e.getMessage(), e);
            }
        }
    }

    boolean isDue(CronSchedule schedule, Instant now) {
        CronExpression cron = CronExpression.parse(schedule.getCronExpression());
        Instant lastTriggered = schedule.getLastTriggeredAt();

        if (lastTriggered == null) {
            // Never triggered before — trigger immediately
            return true;
        }

        LocalDateTime lastLocal = LocalDateTime.ofInstant(lastTriggered, ZoneId.systemDefault());
        LocalDateTime nextFire = cron.next(lastLocal);
        if (nextFire == null) {
            return false;
        }
        Instant nextFireInstant = nextFire.atZone(ZoneId.systemDefault()).toInstant();
        return !now.isBefore(nextFireInstant);
    }

    /**
     * Computes the next N trigger times for all enabled schedules.
     *
     * @param count number of future triggers to compute per schedule
     * @return list of schedule info maps with upcoming trigger times
     */
    public List<Map<String, Object>> getUpcomingTriggers(int count) {
        List<CronSchedule> schedules = scheduleRepository.findByEnabled(true);
        List<Map<String, Object>> result = new ArrayList<>();

        for (CronSchedule schedule : schedules) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("jobName", schedule.getJobName());
            entry.put("cronExpression", schedule.getCronExpression());
            entry.put("targetNode", schedule.getTargetNode());
            entry.put("enabled", schedule.isEnabled());
            entry.put("nodeId", nodeId);

            List<String> upcoming = new ArrayList<>();
            try {
                CronExpression cron = CronExpression.parse(schedule.getCronExpression());
                LocalDateTime cursor = LocalDateTime.now();
                for (int i = 0; i < count; i++) {
                    LocalDateTime next = cron.next(cursor);
                    if (next == null) break;
                    upcoming.add(next.atZone(ZoneId.systemDefault()).toInstant().toString());
                    cursor = next;
                }
            } catch (Exception e) {
                log.warn("Invalid cron expression '{}' for job '{}'", schedule.getCronExpression(), schedule.getJobName());
            }
            entry.put("upcoming", upcoming);
            result.add(entry);
        }
        return result;
    }

    public List<CronSchedule> getAllSchedules() {
        return scheduleRepository.findAll();
    }

    public String getNodeId() {
        return nodeId;
    }

    private String resolveNodeId() {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            return hostname != null ? hostname : "node-" + ProcessHandle.current().pid();
        } catch (Exception e) {
            return "node-" + ProcessHandle.current().pid();
        }
    }
}
