package com.fluffy.batch.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for cloud-native job execution.
 *
 * <p>When {@code fluffy.batch.cloud-native.enabled=true}, jobs whose
 * {@code executionMode} is {@code CLOUD_NATIVE} will be dispatched to the
 * configured endpoint instead of being executed in-process.</p>
 *
 * <p>Example configuration:</p>
 * <pre>
 * fluffy.batch.cloud-native.enabled=true
 * fluffy.batch.cloud-native.endpoint=http://airflow.internal/api/v1/dags/fluffy/dagRuns
 * fluffy.batch.cloud-native.callback-url=http://my-app:8080/api/jobs/callback
 * </pre>
 */
@ConfigurationProperties(prefix = "fluffy.batch.cloud-native")
public class CloudNativeProperties {

    /** Whether cloud-native execution is enabled. */
    private boolean enabled = false;

    /**
     * The HTTP endpoint to which job dispatch requests are POSTed.
     * This could be a Kubernetes API, Airflow REST API, AWS Batch endpoint, etc.
     */
    private String endpoint;

    /**
     * Optional callback URL that is included in the dispatch payload so the
     * external orchestrator can POST status updates back.
     */
    private String callbackUrl;

    /** Connection timeout in seconds for outbound HTTP requests. */
    private long connectTimeoutSeconds = 10;

    /** Request timeout in seconds for outbound HTTP requests. */
    private long requestTimeoutSeconds = 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public long getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public void setConnectTimeoutSeconds(long connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    public long getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public void setRequestTimeoutSeconds(long requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }
}
