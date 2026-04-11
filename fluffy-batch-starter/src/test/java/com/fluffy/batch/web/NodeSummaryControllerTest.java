package com.fluffy.batch.web;

import com.fluffy.batch.dashboard.DashboardProperties;
import com.fluffy.batch.model.JobExecution;
import com.fluffy.batch.persistence.JobExecutionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NodeSummaryControllerTest {

    private final JobExecutionRepository repository = mock(JobExecutionRepository.class);

    @Test
    void shouldReturnSummaryWithCorrectCounts() {
        DashboardProperties props = new DashboardProperties();
        props.setEnabled(true);
        NodeSummaryController controller = new NodeSummaryController(repository, props);

        JobExecution starting = new JobExecution();
        starting.setStatus(BatchStatus.STARTING);
        JobExecution started = new JobExecution();
        started.setStatus(BatchStatus.STARTED);
        JobExecution completed = new JobExecution();
        completed.setStatus(BatchStatus.COMPLETED);
        JobExecution failed = new JobExecution();
        failed.setStatus(BatchStatus.FAILED);

        when(repository.findAll()).thenReturn(List.of(starting, started, completed, failed));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);

        var summary = controller.getSummary(request);

        assertThat(summary.queued()).isEqualTo(1);
        assertThat(summary.running()).isEqualTo(1);
        assertThat(summary.succeeded()).isEqualTo(1);
        assertThat(summary.failed()).isEqualTo(1);
        assertThat(summary.dashboardAvailable()).isTrue();
        assertThat(summary.dashboardUrl()).isEqualTo("http://localhost:8080/fluffy-dashboard");
    }

    @Test
    void shouldReturnSummaryWithDashboardDisabled() {
        DashboardProperties props = new DashboardProperties();
        props.setEnabled(false);
        NodeSummaryController controller = new NodeSummaryController(repository, props);

        when(repository.findAll()).thenReturn(List.of());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("node1");
        request.setServerPort(9090);

        var summary = controller.getSummary(request);

        assertThat(summary.queued()).isZero();
        assertThat(summary.running()).isZero();
        assertThat(summary.succeeded()).isZero();
        assertThat(summary.failed()).isZero();
        assertThat(summary.dashboardAvailable()).isFalse();
        assertThat(summary.dashboardUrl()).isNull();
    }

    @Test
    void shouldReturnZeroCountsWhenNoExecutions() {
        DashboardProperties props = new DashboardProperties();
        props.setEnabled(true);
        NodeSummaryController controller = new NodeSummaryController(repository, props);
        when(repository.findAll()).thenReturn(List.of());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("https");
        request.setServerName("host");
        request.setServerPort(443);

        var summary = controller.getSummary(request);

        assertThat(summary.queued()).isZero();
        assertThat(summary.running()).isZero();
        assertThat(summary.succeeded()).isZero();
        assertThat(summary.failed()).isZero();
        assertThat(summary.dashboardAvailable()).isTrue();
        assertThat(summary.dashboardUrl()).isEqualTo("https://host:443/fluffy-dashboard");
    }

    @Test
    void shouldHandleMultipleExecutionsOfSameStatus() {
        DashboardProperties props = new DashboardProperties();
        props.setEnabled(false);
        NodeSummaryController controller = new NodeSummaryController(repository, props);

        JobExecution c1 = new JobExecution();
        c1.setStatus(BatchStatus.COMPLETED);
        JobExecution c2 = new JobExecution();
        c2.setStatus(BatchStatus.COMPLETED);
        JobExecution c3 = new JobExecution();
        c3.setStatus(BatchStatus.COMPLETED);

        when(repository.findAll()).thenReturn(List.of(c1, c2, c3));

        MockHttpServletRequest request = new MockHttpServletRequest();
        var summary = controller.getSummary(request);

        assertThat(summary.succeeded()).isEqualTo(3);
        assertThat(summary.queued()).isZero();
    }
}
