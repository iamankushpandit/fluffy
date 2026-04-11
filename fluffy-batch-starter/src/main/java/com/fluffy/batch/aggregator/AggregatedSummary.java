package com.fluffy.batch.aggregator;

import com.fluffy.batch.api.NodeSummary;

import java.util.List;

/**
 * Immutable snapshot of the aggregated state across all discovered nodes.
 *
 * @param totalQueued     sum of queued jobs across all nodes
 * @param totalRunning    sum of running jobs across all nodes
 * @param totalSucceeded  sum of succeeded jobs across all nodes
 * @param totalFailed     sum of failed jobs across all nodes
 * @param nodeSummaries   per-node breakdown
 */
public record AggregatedSummary(
        long totalQueued,
        long totalRunning,
        long totalSucceeded,
        long totalFailed,
        List<NodeSummary> nodeSummaries
) {
    /** Computes an aggregate from a list of per-node summaries. */
    public static AggregatedSummary from(List<NodeSummary> summaries) {
        long queued = summaries.stream().mapToLong(NodeSummary::queued).sum();
        long running = summaries.stream().mapToLong(NodeSummary::running).sum();
        long succeeded = summaries.stream().mapToLong(NodeSummary::succeeded).sum();
        long failed = summaries.stream().mapToLong(NodeSummary::failed).sum();
        return new AggregatedSummary(queued, running, succeeded, failed, List.copyOf(summaries));
    }
}
