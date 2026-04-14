package com.fluffy.aggregator;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AggregatedSummaryTest {

    @Test
    void shouldAggregateFromMultipleNodes() {
        NodeSummary n1 = new NodeSummary("node-1", 2, 3, 10, 1, true, "http://n1/dash");
        NodeSummary n2 = new NodeSummary("node-2", 1, 5, 20, 3, false, null);

        AggregatedSummary agg = AggregatedSummary.from(List.of(n1, n2));

        assertThat(agg.totalQueued()).isEqualTo(3);
        assertThat(agg.totalRunning()).isEqualTo(8);
        assertThat(agg.totalSucceeded()).isEqualTo(30);
        assertThat(agg.totalFailed()).isEqualTo(4);
        assertThat(agg.nodeSummaries()).hasSize(2);
    }

    @Test
    void shouldHandleEmptyList() {
        AggregatedSummary agg = AggregatedSummary.from(List.of());

        assertThat(agg.totalQueued()).isZero();
        assertThat(agg.totalRunning()).isZero();
        assertThat(agg.totalSucceeded()).isZero();
        assertThat(agg.totalFailed()).isZero();
        assertThat(agg.nodeSummaries()).isEmpty();
    }

    @Test
    void shouldReturnUnmodifiableNodeList() {
        NodeSummary n1 = new NodeSummary("n1", 0, 0, 0, 0, false, null);
        AggregatedSummary agg = AggregatedSummary.from(List.of(n1));

        assertThat(agg.nodeSummaries()).isUnmodifiable();
    }

    @Test
    void shouldHandleSingleNode() {
        NodeSummary n = new NodeSummary("solo", 4, 2, 50, 0, true, "http://solo/dash");

        AggregatedSummary agg = AggregatedSummary.from(List.of(n));

        assertThat(agg.totalQueued()).isEqualTo(4);
        assertThat(agg.totalRunning()).isEqualTo(2);
        assertThat(agg.totalSucceeded()).isEqualTo(50);
        assertThat(agg.totalFailed()).isZero();
        assertThat(agg.nodeSummaries()).hasSize(1);
    }
}
