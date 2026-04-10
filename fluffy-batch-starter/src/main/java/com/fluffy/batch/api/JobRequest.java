package com.fluffy.batch.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class JobRequest {
    private Map<String, String> parameters;
    private String arguments;
    private String requestedBy;

    public JobRequest() {}

    public Map<String, String> getParameters() {
        return parameters != null ? parameters : Collections.emptyMap();
    }
    public void setParameters(Map<String, String> parameters) { this.parameters = parameters; }

    public String getArguments() { return arguments; }
    public void setArguments(String arguments) { this.arguments = arguments; }

    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }
}
