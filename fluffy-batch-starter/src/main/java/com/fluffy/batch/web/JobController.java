package com.fluffy.batch.web;

import com.fluffy.batch.api.JobRequest;
import com.fluffy.batch.engine.JobDefinition;
import com.fluffy.batch.engine.JobLauncher;
import com.fluffy.batch.engine.JobNotFoundException;
import com.fluffy.batch.engine.JobRegistry;
import com.fluffy.batch.model.JobExecution;
import com.fluffy.batch.model.JobStatusResponse;
import com.fluffy.batch.persistence.JobExecutionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;
    private final JobExecutionRepository executionRepository;

    public JobController(JobLauncher jobLauncher, JobRegistry jobRegistry,
                         JobExecutionRepository executionRepository) {
        this.jobLauncher = jobLauncher;
        this.jobRegistry = jobRegistry;
        this.executionRepository = executionRepository;
    }

    @PostMapping("/{jobName}/start")
    public ResponseEntity<JobStatusResponse> startJob(
            @PathVariable String jobName,
            @RequestBody(required = false) JobRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId) {

        if (request == null) {
            request = new JobRequest(null, null, userId);
        } else if (request.requestedBy() == null) {
            request = request.withRequestedBy(userId);
        }

        Long executionId = jobLauncher.launch(jobName, request);
        JobExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new JobNotFoundException("Execution not found: " + executionId));
        return ResponseEntity.ok(JobStatusResponse.from(execution));
    }

    @GetMapping("/{executionId}/status")
    public ResponseEntity<JobStatusResponse> getStatus(@PathVariable Long executionId) {
        JobExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new JobNotFoundException("Execution not found: " + executionId));
        return ResponseEntity.ok(JobStatusResponse.from(execution));
    }

    @PostMapping("/{executionId}/stop")
    public ResponseEntity<JobStatusResponse> stopJob(@PathVariable Long executionId) {
        jobLauncher.stop(executionId);
        JobExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new JobNotFoundException("Execution not found: " + executionId));
        return ResponseEntity.ok(JobStatusResponse.from(execution));
    }

    @PostMapping("/{executionId}/retry")
    public ResponseEntity<JobStatusResponse> retryJob(@PathVariable Long executionId) {
        Long newExecutionId = jobLauncher.retry(executionId);
        JobExecution execution = executionRepository.findById(newExecutionId)
                .orElseThrow(() -> new JobNotFoundException("Execution not found: " + newExecutionId));
        return ResponseEntity.ok(JobStatusResponse.from(execution));
    }

    @GetMapping("/executions")
    public ResponseEntity<List<JobStatusResponse>> listExecutions() {
        List<JobStatusResponse> responses = executionRepository.findAllByOrderByStartTimeDesc()
                .stream()
                .map(JobStatusResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/registered")
    public ResponseEntity<List<Map<String, Object>>> listRegisteredJobs() {
        List<Map<String, Object>> jobs = jobRegistry.getAll().stream()
                .map(def -> {
                    Map<String, Object> info = new LinkedHashMap<>();
                    info.put("name", def.name());
                    info.put("description", def.description());
                    info.put("maxConcurrency", def.maxConcurrency());
                    info.put("async", def.async());
                    info.put("timeoutSeconds", def.timeoutSeconds());
                    info.put("requiredParams", def.requiredParams());
                    info.put("executionMode", def.executionMode().name());
                    return info;
                })
                .toList();
        return ResponseEntity.ok(jobs);
    }
}
