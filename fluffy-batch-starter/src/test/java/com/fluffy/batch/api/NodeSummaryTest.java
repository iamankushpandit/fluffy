package com.fluffy.batch.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NodeSummaryTest {

    @Test
    void shouldCreateNodeSummaryWithAllFields() {
        NodeSummary summary = new NodeSummary(
                "node-1", 5, 3, 100, 2, true, "http://node1:8080/fluffy-dashboard");

        assertThat(summary.nodeId()).isEqualTo("node-1");
        assertThat(summary.queued()).isEqualTo(5);
        assertThat(summary.running()).isEqualTo(3);
        assertThat(summary.succeeded()).isEqualTo(100);
        assertThat(summary.failed()).isEqualTo(2);
        assertThat(summary.dashboardAvailable()).isTrue();
        assertThat(summary.dashboardUrl()).isEqualTo("http://node1:8080/fluffy-dashboard");
    }

    @Test
    void shouldCreateNodeSummaryWithNullDashboardUrl() {
        NodeSummary summary = new NodeSummary("node-2", 0, 0, 0, 0, false, null);

        assertThat(summary.dashboardAvailable()).isFalse();
        assertThat(summary.dashboardUrl()).isNull();
    }

    @Test
    void shouldSupportRecordEquality() {
        NodeSummary a = new NodeSummary("n1", 1, 2, 3, 4, true, "http://a");
        NodeSummary b = new NodeSummary("n1", 1, 2, 3, 4, true, "http://a");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void shouldSupportRecordToString() {
        NodeSummary summary = new NodeSummary("n1", 0, 0, 0, 0, false, null);
        assertThat(summary.toString()).contains("n1");
    }
}
