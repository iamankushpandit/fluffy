package com.fluffy.batch.persistence;

import com.fluffy.batch.model.JobExecution;
import com.fluffy.batch.model.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobExecutionRepository extends JpaRepository<JobExecution, Long> {
    List<JobExecution> findByJobNameOrderByStartTimeDesc(String jobName);
    List<JobExecution> findByStatus(JobStatus status);
    List<JobExecution> findAllByOrderByStartTimeDesc();
}
