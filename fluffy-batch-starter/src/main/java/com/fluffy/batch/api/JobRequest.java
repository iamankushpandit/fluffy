package com.fluffy.batch.api;

import java.util.Map;

/**
 * Immutable request to launch a batch job.
 *
 * @param parameters  key-value parameters forwarded to the job handler
 * @param arguments   free-form argument string
 * @param requestedBy identity of the caller (defaults to "anonymous")
 */
public record JobRequest(
        Map<String, String> parameters,
        String arguments,
        String requestedBy
) {
    /** Canonical constructor – stores an unmodifiable copy of the parameter map. */
    public JobRequest {
        parameters = parameters != null ? Map.copyOf(parameters) : Map.of();
    }

    /** No-arg convenience constructor used for deserialization and default launches. */
    public JobRequest() {
        this(null, null, null);
    }

    /** Returns a new request with the given {@code requestedBy} value. */
    public JobRequest withRequestedBy(String requestedBy) {
        return new JobRequest(this.parameters, this.arguments, requestedBy);
    }
}
