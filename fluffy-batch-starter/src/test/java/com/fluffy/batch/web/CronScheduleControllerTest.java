package com.fluffy.batch.web;

import com.fluffy.batch.engine.CronJobScheduler;
import com.fluffy.batch.model.CronSchedule;
import com.fluffy.batch.persistence.CronScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CronScheduleControllerTest {

    private CronScheduleRepository scheduleRepository;
    private CronJobScheduler cronJobScheduler;
    private CronScheduleController controller;

    @BeforeEach
    void setUp() {
        scheduleRepository = mock(CronScheduleRepository.class);
        cronJobScheduler = mock(CronJobScheduler.class);
        controller = new CronScheduleController(scheduleRepository, cronJobScheduler);
    }

    @Test
    void shouldListSchedules() {
        CronSchedule s = new CronSchedule("my-job", "0 0 * * * *");
        s.setId(1L);
        s.setCreatedAt(Instant.now());
        when(cronJobScheduler.getAllSchedules()).thenReturn(List.of(s));
        when(cronJobScheduler.getNodeId()).thenReturn("test-node");

        ResponseEntity<List<Map<String, Object>>> response = controller.listSchedules();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0)).containsEntry("jobName", "my-job");
    }

    @Test
    void shouldCreateNewSchedule() {
        when(scheduleRepository.findByJobName("new-job")).thenReturn(Optional.empty());
        CronSchedule saved = new CronSchedule("new-job", "0 0 * * * *");
        saved.setId(1L);
        when(scheduleRepository.save(any())).thenReturn(saved);
        when(cronJobScheduler.getNodeId()).thenReturn("test-node");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jobName", "new-job");
        body.put("cronExpression", "0 0 * * * *");

        ResponseEntity<Map<String, Object>> response = controller.createOrUpdateSchedule(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("jobName", "new-job");
    }

    @Test
    void shouldUpdateExistingSchedule() {
        CronSchedule existing = new CronSchedule("existing-job", "0 0 * * * *");
        existing.setId(1L);
        when(scheduleRepository.findByJobName("existing-job")).thenReturn(Optional.of(existing));
        when(scheduleRepository.save(any())).thenReturn(existing);
        when(cronJobScheduler.getNodeId()).thenReturn("test-node");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jobName", "existing-job");
        body.put("cronExpression", "0 30 * * * *");

        ResponseEntity<Map<String, Object>> response = controller.createOrUpdateSchedule(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(scheduleRepository).save(argThat(s -> s.getCronExpression().equals("0 30 * * * *")));
    }

    @Test
    void shouldRejectMissingJobName() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cronExpression", "0 0 * * * *");

        ResponseEntity<Map<String, Object>> response = controller.createOrUpdateSchedule(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRejectMissingCronExpression() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jobName", "my-job");

        ResponseEntity<Map<String, Object>> response = controller.createOrUpdateSchedule(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRejectInvalidCronExpression() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jobName", "my-job");
        body.put("cronExpression", "invalid-cron");

        ResponseEntity<Map<String, Object>> response = controller.createOrUpdateSchedule(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldDeleteSchedule() {
        CronSchedule existing = new CronSchedule("delete-me", "0 0 * * * *");
        when(scheduleRepository.findByJobName("delete-me")).thenReturn(Optional.of(existing));

        ResponseEntity<Map<String, Object>> response = controller.deleteSchedule("delete-me");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(scheduleRepository).deleteByJobName("delete-me");
    }

    @Test
    void shouldReturn404ForNonExistentDelete() {
        when(scheduleRepository.findByJobName("not-found")).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.deleteSchedule("not-found");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldGetUpcoming() {
        Map<String, Object> entry = Map.of("jobName", "my-job", "upcoming", List.of("2025-01-01T00:00:00Z"));
        when(cronJobScheduler.getUpcomingTriggers(10)).thenReturn(List.of(entry));

        ResponseEntity<List<Map<String, Object>>> response = controller.getUpcoming(10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void shouldCreateScheduleWithTargetNode() {
        when(scheduleRepository.findByJobName("targeted-job")).thenReturn(Optional.empty());
        CronSchedule saved = new CronSchedule("targeted-job", "0 0 * * * *");
        saved.setId(1L);
        saved.setTargetNode("node-1");
        when(scheduleRepository.save(any())).thenReturn(saved);
        when(cronJobScheduler.getNodeId()).thenReturn("test-node");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jobName", "targeted-job");
        body.put("cronExpression", "0 0 * * * *");
        body.put("targetNode", "node-1");

        ResponseEntity<Map<String, Object>> response = controller.createOrUpdateSchedule(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(scheduleRepository).save(argThat(s -> "node-1".equals(s.getTargetNode())));
    }

    @Test
    void shouldRejectBlankJobName() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jobName", "  ");
        body.put("cronExpression", "0 0 * * * *");

        ResponseEntity<Map<String, Object>> response = controller.createOrUpdateSchedule(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRejectBlankCronExpression() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jobName", "my-job");
        body.put("cronExpression", "  ");

        ResponseEntity<Map<String, Object>> response = controller.createOrUpdateSchedule(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
