package com.fluffy.batch.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Tracks node liveness for multi-node fault detection.
 * Each application node periodically updates its heartbeat timestamp.
 */
@Entity
@Table(name = "node_heartbeat")
public class NodeHeartbeat {

    @Id
    private String nodeId;

    @Column(nullable = false)
    private Instant lastHeartbeat;

    @Column(nullable = false)
    private String status;

    public NodeHeartbeat() {
    }

    public NodeHeartbeat(String nodeId) {
        this.nodeId = nodeId;
        this.lastHeartbeat = Instant.now();
        this.status = "ALIVE";
    }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public Instant getLastHeartbeat() { return lastHeartbeat; }
    public void setLastHeartbeat(Instant lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
