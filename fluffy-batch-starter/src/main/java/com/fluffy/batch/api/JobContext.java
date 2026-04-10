package com.fluffy.batch.api;

import java.util.Collections;
import java.util.Map;

public class JobContext {
    private final Long executionId;
    private final String jobName;
    private final String requestedBy;
    private final Map<String, String> parameters;
    private final String arguments;
    private volatile boolean stopRequested;

    public JobContext(Long executionId, String jobName, String requestedBy,
                      Map<String, String> parameters, String arguments) {
        this.executionId = executionId;
        this.jobName = jobName;
        this.requestedBy = requestedBy;
        this.parameters = parameters != null ? Collections.unmodifiableMap(parameters) : Collections.emptyMap();
        this.arguments = arguments;
        this.stopRequested = false;
    }

    public Long getExecutionId() { return executionId; }
    public String getJobName() { return jobName; }
    public String getRequestedBy() { return requestedBy; }
    public Map<String, String> getParameters() { return parameters; }
    public String getArguments() { return arguments; }
    public boolean isStopRequested() { return stopRequested; }

    public String getParam(String key) {
        return parameters.getOrDefault(key, null);
    }

    public String requireParam(String key) {
        String val = parameters.get(key);
        if (val == null) {
            throw new IllegalArgumentException("Required param missing: " + key);
        }
        return val;
    }

    public void checkInterrupted() throws InterruptedException {
        if (stopRequested || Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Job stop requested");
        }
    }

    public void markStopRequested() {
        this.stopRequested = true;
    }
}
