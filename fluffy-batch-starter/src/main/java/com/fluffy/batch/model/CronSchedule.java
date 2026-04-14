package com.fluffy.batch.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Persistent cron schedule for a batch job.
 * Each entry defines a cron expression that triggers
 * the named job on a periodic schedule.
 */
@Entity
@Table(name = "cron_schedule")
public class CronSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String jobName;

    @Column(nullable = false)
    private String cronExpression;

    /** Optional target node identifier. When set, only the matching node fires the job. */
    private String targetNode;

    private boolean enabled = true;

    private Instant createdAt;

    private Instant lastTriggeredAt;

    public CronSchedule() {}

    public CronSchedule(String jobName, String cronExpression) {
        this.jobName = jobName;
        this.cronExpression = cronExpression;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }

    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }

    public String getTargetNode() { return targetNode; }
    public void setTargetNode(String targetNode) { this.targetNode = targetNode; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getLastTriggeredAt() { return lastTriggeredAt; }
    public void setLastTriggeredAt(Instant lastTriggeredAt) { this.lastTriggeredAt = lastTriggeredAt; }
}
