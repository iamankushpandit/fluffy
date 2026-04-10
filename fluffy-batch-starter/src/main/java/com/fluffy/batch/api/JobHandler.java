package com.fluffy.batch.api;

@FunctionalInterface
public interface JobHandler {
    void execute(JobContext context) throws Exception;
}
