package com.fluffy.batch.web;

import com.fluffy.batch.engine.CloudNativeExecutionStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST endpoint that receives status callbacks from external cloud-native
 * orchestrators (Kubernetes, Airflow, AWS Batch, etc.).
 *
 * <p>The external orchestrator should POST to {@code /api/jobs/callback}
 * with a JSON body containing {@code executionId}, {@code status}
 * ({@code COMPLETED}, {@code FAILED}, or {@code STOPPED}), and an optional
 * {@code errorMessage}.</p>
 *
 * <p>This controller is only active when cloud-native execution is enabled
 * ({@code fluffy.batch.cloud-native.enabled=true}).</p>
 */
@RestController
@RequestMapping("/api/jobs")
@ConditionalOnBean(CloudNativeExecutionStrategy.class)
public class CloudNativeCallbackController {

    private final CloudNativeExecutionStrategy strategy;

    public CloudNativeCallbackController(CloudNativeExecutionStrategy strategy) {
        this.strategy = strategy;
    }

    @PostMapping("/callback")
    public ResponseEntity<Map<String, String>> handleCallback(@RequestBody Map<String, String> body) {
        String executionIdStr = body.get("executionId");
        String status = body.get("status");

        if (executionIdStr == null || status == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "executionId and status are required"));
        }

        Long executionId;
        try {
            executionId = Long.valueOf(executionIdStr);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "executionId must be a number"));
        }

        String errorMessage = body.get("errorMessage");
        strategy.handleCallback(executionId, status, errorMessage);

        return ResponseEntity.ok(Map.of("status", "accepted"));
    }
}
