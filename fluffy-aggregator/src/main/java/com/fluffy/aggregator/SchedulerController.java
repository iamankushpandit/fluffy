package com.fluffy.aggregator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * REST endpoints for managing and viewing cron schedules across all Fluffy nodes.
 * Proxies requests to individual nodes and aggregates schedule data for the
 * Outlook-style calendar view.
 */
@RestController
@RequestMapping("/api/scheduler")
public class SchedulerController {

    private static final Logger log = LoggerFactory.getLogger(SchedulerController.class);

    private final NodeDiscoveryService discoveryService;
    private final RestClient restClient;

    public SchedulerController(NodeDiscoveryService discoveryService, RestClient restClient) {
        this.discoveryService = discoveryService;
        this.restClient = restClient;
    }

    /** Aggregates schedules from all nodes. */
    @GetMapping("/schedules")
    public ResponseEntity<List<Map<String, Object>>> getAllSchedules() {
        List<Map<String, Object>> allSchedules = new ArrayList<>();
        for (String nodeUrl : discoveryService.getNodes()) {
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> nodeSchedules = restClient.get()
                        .uri(nodeUrl + "/api/jobs/schedules")
                        .retrieve()
                        .body(List.class);
                if (nodeSchedules != null) {
                    for (Map<String, Object> schedule : nodeSchedules) {
                        schedule.put("nodeUrl", nodeUrl);
                    }
                    allSchedules.addAll(nodeSchedules);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch schedules from {}: {}", nodeUrl, e.getMessage());
            }
        }
        return ResponseEntity.ok(allSchedules);
    }

    /** Aggregates upcoming triggers from all nodes for the Outlook-style view. */
    @GetMapping("/upcoming")
    public ResponseEntity<List<Map<String, Object>>> getUpcomingTriggers(
            @RequestParam(defaultValue = "48") int count) {
        List<Map<String, Object>> allUpcoming = new ArrayList<>();
        for (String nodeUrl : discoveryService.getNodes()) {
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> upcoming = restClient.get()
                        .uri(nodeUrl + "/api/jobs/schedules/upcoming?count=" + count)
                        .retrieve()
                        .body(List.class);
                if (upcoming != null) {
                    for (Map<String, Object> entry : upcoming) {
                        entry.put("nodeUrl", nodeUrl);
                    }
                    allUpcoming.addAll(upcoming);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch upcoming triggers from {}: {}", nodeUrl, e.getMessage());
            }
        }
        return ResponseEntity.ok(allUpcoming);
    }

    /** Creates or updates a schedule on a specific node. */
    @PostMapping("/schedules")
    public ResponseEntity<Map<String, Object>> createSchedule(@RequestBody Map<String, Object> body) {
        String nodeUrl = (String) body.get("nodeUrl");
        if (nodeUrl == null || nodeUrl.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "nodeUrl is required"));
        }

        // Strip nodeUrl from the payload before forwarding
        Map<String, Object> forwardBody = new LinkedHashMap<>(body);
        forwardBody.remove("nodeUrl");

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = restClient.post()
                    .uri(nodeUrl + "/api/jobs/schedules")
                    .body(forwardBody)
                    .retrieve()
                    .body(Map.class);
            if (result != null) {
                result.put("nodeUrl", nodeUrl);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to create schedule on {}: {}", nodeUrl, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to create schedule: " + e.getMessage()));
        }
    }

    /** Deletes a schedule from a specific node. */
    @DeleteMapping("/schedules/{jobName}")
    public ResponseEntity<Map<String, Object>> deleteSchedule(
            @PathVariable String jobName,
            @RequestParam String nodeUrl) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = restClient.delete()
                    .uri(nodeUrl + "/api/jobs/schedules/" + jobName)
                    .retrieve()
                    .body(Map.class);
            return ResponseEntity.ok(result != null ? result : Map.of("deleted", jobName));
        } catch (Exception e) {
            log.error("Failed to delete schedule on {}: {}", nodeUrl, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to delete schedule: " + e.getMessage()));
        }
    }

    /** Returns registered jobs from all nodes for the schedule creation form. */
    @GetMapping("/registered-jobs")
    public ResponseEntity<List<Map<String, Object>>> getRegisteredJobs() {
        List<Map<String, Object>> allJobs = new ArrayList<>();
        for (String nodeUrl : discoveryService.getNodes()) {
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> jobs = restClient.get()
                        .uri(nodeUrl + "/api/jobs/registered")
                        .retrieve()
                        .body(List.class);
                if (jobs != null) {
                    for (Map<String, Object> job : jobs) {
                        job.put("nodeUrl", nodeUrl);
                    }
                    allJobs.addAll(jobs);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch registered jobs from {}: {}", nodeUrl, e.getMessage());
            }
        }
        return ResponseEntity.ok(allJobs);
    }
}
