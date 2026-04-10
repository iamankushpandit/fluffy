package com.fluffy.batch.model;

import java.sql.Timestamp;

public class JobStatusResponse {
    private Long jobId;
    private String jobName;
    private String status;
    private String requestedBy;
    private Timestamp startTime;
    private Timestamp endTime;
    private Integer queuePosition;
    private String errorMessage;

    public JobStatusResponse() {}

    public static JobStatusResponse from(JobExecution execution) {
        JobStatusResponse response = new JobStatusResponse();
        response.setJobId(execution.getId());
        response.setJobName(execution.getJobName());
        response.setStatus(execution.getStatus() != null ? execution.getStatus().name() : null);
        response.setRequestedBy(execution.getRequestedBy());
        response.setStartTime(execution.getStartTime());
        response.setEndTime(execution.getEndTime());
        response.setQueuePosition(execution.getQueuePosition());
        response.setErrorMessage(execution.getErrorMessage());
        return response;
    }

    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }

    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }

    public Timestamp getStartTime() { return startTime; }
    public void setStartTime(Timestamp startTime) { this.startTime = startTime; }

    public Timestamp getEndTime() { return endTime; }
    public void setEndTime(Timestamp endTime) { this.endTime = endTime; }

    public Integer getQueuePosition() { return queuePosition; }
    public void setQueuePosition(Integer queuePosition) { this.queuePosition = queuePosition; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
