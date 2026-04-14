package com.fluffy.aggregator;

/**
 * Immutable summary of a single Fluffy node's job metrics and dashboard availability.
 *
 * <p>Mirrors the JSON shape produced by every Fluffy node's
 * {@code GET /api/jobs/summary} endpoint.
 *
 * @param nodeId             unique identifier of the node
 * @param queued             number of jobs currently queued
 * @param running            number of jobs currently running
 * @param succeeded          number of jobs that completed successfully
 * @param failed             number of jobs that failed
 * @param dashboardAvailable whether the node's per-node dashboard UI is enabled
 * @param dashboardUrl       full URL to the node's dashboard (null when unavailable)
 */
public record NodeSummary(
        String nodeId,
        long queued,
        long running,
        long succeeded,
        long failed,
        boolean dashboardAvailable,
        String dashboardUrl
) {}
