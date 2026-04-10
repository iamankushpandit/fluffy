package com.fluffy.batch.engine;

import com.fluffy.batch.api.JobHandler;

/**
 * Immutable definition of a batch job: its name, concurrency limits, timeout,
 * required parameters, and the handler that executes the work.
 */
public record JobDefinition(
        String name,
        String description,
        int maxConcurrency,
        boolean async,
        long timeoutSeconds,
        String[] requiredParams,
        JobHandler handler
) {
    /** Validates invariants. */
    public JobDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Job name must not be blank");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Job handler must not be null");
        }
        if (requiredParams == null) {
            requiredParams = new String[0];
        }
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static class Builder {
        private final String name;
        private String description = "";
        private int maxConcurrency = 1;
        private boolean async = true;
        private long timeoutSeconds = 0;
        private String[] requiredParams = new String[0];
        private JobHandler handler;

        public Builder(String name) {
            this.name = name;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder maxConcurrency(int maxConcurrency) {
            this.maxConcurrency = maxConcurrency;
            return this;
        }

        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        public Builder timeoutSeconds(long timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public Builder requiredParams(String... requiredParams) {
            this.requiredParams = requiredParams;
            return this;
        }

        public Builder handler(JobHandler handler) {
            this.handler = handler;
            return this;
        }

        public JobDefinition build() {
            return new JobDefinition(name, description, maxConcurrency, async,
                    timeoutSeconds, requiredParams, handler);
        }
    }
}
