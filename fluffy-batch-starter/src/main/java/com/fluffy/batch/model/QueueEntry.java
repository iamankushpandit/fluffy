package com.fluffy.batch.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Persistent queue entry for database-backed queue mode.
 * Each row represents a job execution waiting to be processed.
 */
@Entity
@Table(name = "queue_entry")
public class QueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long executionId;

    @Column(nullable = false)
    private String jobName;

    private Integer position;

    @Column(nullable = false)
    private String status;

    private String claimedBy;

    private Instant claimedAt;

    @Column(nullable = false)
    private Instant createdAt;

    public QueueEntry() {
    }

    public QueueEntry(Long executionId, String jobName, Integer position) {
        this.executionId = executionId;
        this.jobName = jobName;
        this.position = position;
        this.status = "QUEUED";
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getExecutionId() { return executionId; }
    public void setExecutionId(Long executionId) { this.executionId = executionId; }

    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getClaimedBy() { return claimedBy; }
    public void setClaimedBy(String claimedBy) { this.claimedBy = claimedBy; }

    public Instant getClaimedAt() { return claimedAt; }
    public void setClaimedAt(Instant claimedAt) { this.claimedAt = claimedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
