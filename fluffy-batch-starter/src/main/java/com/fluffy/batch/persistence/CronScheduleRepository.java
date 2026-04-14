package com.fluffy.batch.persistence;

import com.fluffy.batch.model.CronSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CronScheduleRepository extends JpaRepository<CronSchedule, Long> {

    List<CronSchedule> findByEnabled(boolean enabled);

    Optional<CronSchedule> findByJobName(String jobName);

    void deleteByJobName(String jobName);
}
