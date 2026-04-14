package com.fluffy.batch.web;

import com.fluffy.batch.engine.CronJobScheduler;
import com.fluffy.batch.model.CronSchedule;
import com.fluffy.batch.persistence.CronScheduleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API for managing cron schedules on this node.
 * Registered as a bean by {@link com.fluffy.batch.autoconfigure.CronScheduleAutoConfiguration}.
 */
@ResponseBody
@RequestMapping("/api/jobs/schedules")
public class CronScheduleController {

    private final CronScheduleRepository scheduleRepository;
    private final CronJobScheduler cronJobScheduler;

    public CronScheduleController(CronScheduleRepository scheduleRepository,
                                   CronJobScheduler cronJobScheduler) {
        this.scheduleRepository = scheduleRepository;
        this.cronJobScheduler = cronJobScheduler;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listSchedules() {
        List<Map<String, Object>> result = cronJobScheduler.getAllSchedules().stream()
                .map(this::toMap)
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrUpdateSchedule(@RequestBody Map<String, Object> body) {
        String jobName = (String) body.get("jobName");
        String cronExpr = (String) body.get("cronExpression");
        String targetNode = (String) body.get("targetNode");
        Boolean enabled = body.containsKey("enabled") ? (Boolean) body.get("enabled") : true;

        if (jobName == null || jobName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "jobName is required"));
        }
        if (cronExpr == null || cronExpr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "cronExpression is required"));
        }

        if (!CronExpression.isValidExpression(cronExpr)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid cron expression: " + cronExpr));
        }

        Optional<CronSchedule> existing = scheduleRepository.findByJobName(jobName);
        CronSchedule schedule;
        if (existing.isPresent()) {
            schedule = existing.get();
            schedule.setCronExpression(cronExpr);
            schedule.setTargetNode(targetNode);
            schedule.setEnabled(enabled);
        } else {
            schedule = new CronSchedule(jobName, cronExpr);
            schedule.setTargetNode(targetNode);
            schedule.setEnabled(enabled);
        }
        schedule = scheduleRepository.save(schedule);

        return ResponseEntity.ok(toMap(schedule));
    }

    @DeleteMapping("/{jobName}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteSchedule(@PathVariable String jobName) {
        Optional<CronSchedule> existing = scheduleRepository.findByJobName(jobName);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        scheduleRepository.deleteByJobName(jobName);
        return ResponseEntity.ok(Map.of("deleted", jobName));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<Map<String, Object>>> getUpcoming(
            @RequestParam(defaultValue = "10") int count) {
        return ResponseEntity.ok(cronJobScheduler.getUpcomingTriggers(count));
    }

    private Map<String, Object> toMap(CronSchedule schedule) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", schedule.getId());
        map.put("jobName", schedule.getJobName());
        map.put("cronExpression", schedule.getCronExpression());
        map.put("targetNode", schedule.getTargetNode());
        map.put("enabled", schedule.isEnabled());
        map.put("createdAt", schedule.getCreatedAt() != null ? schedule.getCreatedAt().toString() : null);
        map.put("lastTriggeredAt", schedule.getLastTriggeredAt() != null ? schedule.getLastTriggeredAt().toString() : null);
        map.put("nodeId", cronJobScheduler.getNodeId());
        return map;
    }
}
