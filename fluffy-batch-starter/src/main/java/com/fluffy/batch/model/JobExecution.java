package com.fluffy.batch.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "job_execution")
public class JobExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String jobName;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    private String requestedBy;

    private Instant startTime;

    private Instant endTime;

    @Column(length = 4096)
    private String parameters;

    @Column(length = 2048)
    private String arguments;

    private Integer queuePosition;

    @Column(length = 4096)
    private String errorMessage;

    public JobExecution() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }

    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }

    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public String getParameters() { return parameters; }
    public void setParameters(String parameters) { this.parameters = parameters; }

    public String getArguments() { return arguments; }
    public void setArguments(String arguments) { this.arguments = arguments; }

    public Integer getQueuePosition() { return queuePosition; }
    public void setQueuePosition(Integer queuePosition) { this.queuePosition = queuePosition; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
