package com.fluffy.batch.engine;

import com.fluffy.batch.api.JobContext;
import com.fluffy.batch.autoconfigure.CloudNativeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Execution strategy that delegates job execution to a cloud-native orchestrator.
 *
 * <p>Instead of running the job handler in-process, this strategy sends an HTTP
 * request to a configurable endpoint (e.g. a Kubernetes API, Airflow DAG trigger,
 * or AWS Batch submit-job endpoint). The external orchestrator is responsible for
 * actually running the containerised job.</p>
 *
 * <p>The request includes the job name, execution ID, parameters, and arguments
 * so the remote executor can identify and configure the work.</p>
 *
 * <p>A callback URL can be configured so the external orchestrator can POST back
 * status updates (COMPLETED / FAILED / STOPPED) to the {@code /api/jobs/callback}
 * endpoint.</p>
 */
public class CloudNativeExecutionStrategy implements ExecutionStrategy {

    private static final Logger log = LoggerFactory.getLogger(CloudNativeExecutionStrategy.class);

    private final CloudNativeProperties properties;
    private final ExecutionCallback callback;
    private final HttpClient httpClient;

    /** Tracks execution IDs that have been dispatched externally. */
    private final Map<Long, String> dispatchedJobs = new ConcurrentHashMap<>();

    public CloudNativeExecutionStrategy(CloudNativeProperties properties,
                                        ExecutionCallback callback) {
        this.properties = properties;
        this.callback = callback;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                .build();
    }

    /* visible for testing */
    CloudNativeExecutionStrategy(CloudNativeProperties properties,
                                 ExecutionCallback callback,
                                 HttpClient httpClient) {
        this.properties = properties;
        this.callback = callback;
        this.httpClient = httpClient;
    }

    @Override
    public ExecutionMode getMode() {
        return ExecutionMode.CLOUD_NATIVE;
    }

    @Override
    public void execute(Long executionId, JobDefinition definition, JobContext context) {
        String endpoint = properties.getEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            log.error("Cloud-native endpoint not configured; failing job {} (executionId={})",
                    definition.name(), executionId);
            callback.onFailed(executionId, definition.name(),
                    "Cloud-native endpoint not configured");
            callback.onFinished(executionId, definition.name(), definition.maxConcurrency());
            return;
        }

        try {
            callback.onStarted(executionId);

            String payload = buildPayload(executionId, definition, context);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(properties.getRequestTimeoutSeconds()))
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                dispatchedJobs.put(executionId, definition.name());
                log.info("Job {} dispatched to cloud-native endpoint (executionId={}, status={})",
                        definition.name(), executionId, response.statusCode());
            } else {
                log.error("Cloud-native dispatch failed for job {} (executionId={}, status={}, body={})",
                        definition.name(), executionId, response.statusCode(), response.body());
                callback.onFailed(executionId, definition.name(),
                        "Cloud-native dispatch failed with status " + response.statusCode());
                callback.onFinished(executionId, definition.name(), definition.maxConcurrency());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            callback.onStopped(executionId, definition.name());
            callback.onFinished(executionId, definition.name(), definition.maxConcurrency());
        } catch (Exception e) {
            log.error("Failed to dispatch job {} to cloud-native endpoint (executionId={}): {}",
                    definition.name(), executionId, e.getMessage(), e);
            callback.onFailed(executionId, definition.name(),
                    "Cloud-native dispatch error: " + e.getMessage());
            callback.onFinished(executionId, definition.name(), definition.maxConcurrency());
        }
    }

    @Override
    public boolean stop(Long executionId) {
        return dispatchedJobs.remove(executionId) != null;
    }

    /**
     * Called by the callback endpoint when the external orchestrator reports
     * that the job has finished.
     */
    public void handleCallback(Long executionId, String status, String errorMessage) {
        String jobName = dispatchedJobs.remove(executionId);
        if (jobName == null) {
            log.warn("Received callback for unknown executionId={}", executionId);
            return;
        }

        switch (status.toUpperCase()) {
            case "COMPLETED" -> callback.onCompleted(executionId, jobName);
            case "FAILED" -> callback.onFailed(executionId, jobName, errorMessage);
            case "STOPPED" -> callback.onStopped(executionId, jobName);
            default -> {
                log.warn("Unknown callback status '{}' for executionId={}", status, executionId);
                callback.onFailed(executionId, jobName, "Unknown callback status: " + status);
            }
        }
    }

    /* package */ Map<Long, String> getDispatchedJobs() {
        return dispatchedJobs;
    }

    private String buildPayload(Long executionId, JobDefinition definition, JobContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"executionId\":").append(executionId).append(",");
        sb.append("\"jobName\":\"").append(escapeJson(definition.name())).append("\",");
        sb.append("\"requestedBy\":\"").append(escapeJson(context.getRequestedBy())).append("\"");

        if (properties.getCallbackUrl() != null && !properties.getCallbackUrl().isBlank()) {
            sb.append(",\"callbackUrl\":\"").append(escapeJson(properties.getCallbackUrl())).append("\"");
        }

        if (context.getArguments() != null) {
            sb.append(",\"arguments\":\"").append(escapeJson(context.getArguments())).append("\"");
        }

        if (!context.getParameters().isEmpty()) {
            sb.append(",\"parameters\":{");
            boolean first = true;
            for (Map.Entry<String, String> entry : context.getParameters().entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(escapeJson(entry.getKey())).append("\":\"")
                  .append(escapeJson(entry.getValue())).append("\"");
                first = false;
            }
            sb.append("}");
        }

        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                     .replace("\"", "\\\"")
                     .replace("\n", "\\n")
                     .replace("\r", "\\r")
                     .replace("\t", "\\t");
    }
}
