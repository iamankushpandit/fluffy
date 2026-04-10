package com.fluffy.batch.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class NodeHeartbeatTest {

    @Test
    void shouldCreateWithNodeId() {
        NodeHeartbeat heartbeat = new NodeHeartbeat("node-1");
        assertThat(heartbeat.getNodeId()).isEqualTo("node-1");
        assertThat(heartbeat.getStatus()).isEqualTo("ALIVE");
        assertThat(heartbeat.getLastHeartbeat()).isNotNull();
    }

    @Test
    void shouldCreateWithDefaultConstructor() {
        NodeHeartbeat heartbeat = new NodeHeartbeat();
        assertThat(heartbeat.getNodeId()).isNull();
        assertThat(heartbeat.getStatus()).isNull();
        assertThat(heartbeat.getLastHeartbeat()).isNull();
    }

    @Test
    void shouldSupportAllSetters() {
        NodeHeartbeat heartbeat = new NodeHeartbeat();
        Instant now = Instant.now();
        heartbeat.setNodeId("node-2");
        heartbeat.setLastHeartbeat(now);
        heartbeat.setStatus("STALE");

        assertThat(heartbeat.getNodeId()).isEqualTo("node-2");
        assertThat(heartbeat.getLastHeartbeat()).isEqualTo(now);
        assertThat(heartbeat.getStatus()).isEqualTo("STALE");
    }
}
