package com.fluffy.batch.engine;

import com.fluffy.batch.api.JobHandler;

public class JobDefinition {
    private final String name;
    private final String description;
    private final int maxConcurrency;
    private final boolean async;
    private final long timeoutSeconds;
    private final String[] requiredParams;
    private final JobHandler handler;

    private JobDefinition(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.maxConcurrency = builder.maxConcurrency;
        this.async = builder.async;
        this.timeoutSeconds = builder.timeoutSeconds;
        this.requiredParams = builder.requiredParams;
        this.handler = builder.handler;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getMaxConcurrency() { return maxConcurrency; }
    public boolean isAsync() { return async; }
    public long getTimeoutSeconds() { return timeoutSeconds; }
    public String[] getRequiredParams() { return requiredParams; }
    public JobHandler getHandler() { return handler; }

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
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Job name must not be blank");
            }
            if (handler == null) {
                throw new IllegalArgumentException("Job handler must not be null");
            }
            return new JobDefinition(this);
        }
    }
}
