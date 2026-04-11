package com.fluffy.batch.web;

import com.fluffy.batch.api.NodeSummary;
import com.fluffy.batch.dashboard.DashboardProperties;
import com.fluffy.batch.model.JobExecution;
import com.fluffy.batch.persistence.JobExecutionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.batch.core.BatchStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes a lightweight summary endpoint so that an aggregator can poll each
 * node for its current job metrics and dashboard availability.
 */
@RestController
@RequestMapping("/api/jobs")
public class NodeSummaryController {

    private final JobExecutionRepository executionRepository;
    private final DashboardProperties dashboardProperties;
    private final String nodeId;

    public NodeSummaryController(JobExecutionRepository executionRepository,
                                 DashboardProperties dashboardProperties) {
        this.executionRepository = executionRepository;
        this.dashboardProperties = dashboardProperties;
        this.nodeId = System.getProperty("fluffy.node.id",
                System.getenv().getOrDefault("HOSTNAME", "unknown"));
    }

    @GetMapping("/summary")
    public NodeSummary getSummary(HttpServletRequest request) {
        List<JobExecution> executions = executionRepository.findAll();

        long queued = executions.stream()
                .filter(e -> e.getStatus() == BatchStatus.STARTING)
                .count();
        long running = executions.stream()
                .filter(e -> e.getStatus() == BatchStatus.STARTED)
                .count();
        long succeeded = executions.stream()
                .filter(e -> e.getStatus() == BatchStatus.COMPLETED)
                .count();
        long failed = executions.stream()
                .filter(e -> e.getStatus() == BatchStatus.FAILED)
                .count();

        boolean dashboardAvailable = dashboardProperties.isEnabled();
        String dashboardUrl = null;
        if (dashboardAvailable) {
            String baseUrl = request.getScheme() + "://" + request.getServerName()
                    + ":" + request.getServerPort();
            dashboardUrl = baseUrl + dashboardProperties.getPath();
        }

        return new NodeSummary(nodeId, queued, running, succeeded, failed,
                dashboardAvailable, dashboardUrl);
    }
}
