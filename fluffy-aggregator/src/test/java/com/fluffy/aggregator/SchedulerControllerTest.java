package com.fluffy.aggregator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SchedulerControllerTest {

    private NodeDiscoveryService discoveryService;
    private RestClient restClient;
    private SchedulerController controller;

    @BeforeEach
    void setUp() {
        discoveryService = mock(NodeDiscoveryService.class);
        restClient = mock(RestClient.class);
        controller = new SchedulerController(discoveryService, restClient);
    }

    @Test
    void shouldReturnEmptySchedulesWhenNoNodes() {
        when(discoveryService.getNodes()).thenReturn(List.of());

        ResponseEntity<List<Map<String, Object>>> response = controller.getAllSchedules();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void shouldReturnEmptyUpcomingWhenNoNodes() {
        when(discoveryService.getNodes()).thenReturn(List.of());

        ResponseEntity<List<Map<String, Object>>> response = controller.getUpcomingTriggers(10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void shouldRejectCreateWithoutNodeUrl() {
        Map<String, Object> body = Map.of("jobName", "test", "cronExpression", "0 0 * * * *");

        ResponseEntity<Map<String, Object>> response = controller.createSchedule(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturnEmptyRegisteredJobsWhenNoNodes() {
        when(discoveryService.getNodes()).thenReturn(List.of());

        ResponseEntity<List<Map<String, Object>>> response = controller.getRegisteredJobs();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldAggregateSchedulesFromMultipleNodes() {
        when(discoveryService.getNodes()).thenReturn(List.of("http://node1:8080", "http://node2:8080"));

        // Mock restClient chain — each get() call returns a fresh chain
        RestClient.RequestHeadersUriSpec getSpec1 = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec1 = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec1 = mock(RestClient.ResponseSpec.class);

        RestClient.RequestHeadersUriSpec getSpec2 = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec2 = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec2 = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(getSpec1).thenReturn(getSpec2);
        when(getSpec1.uri(anyString())).thenReturn(headersSpec1);
        when(headersSpec1.retrieve()).thenReturn(responseSpec1);
        when(responseSpec1.body(eq(List.class)))
                .thenReturn(List.of(new java.util.HashMap<>(Map.of("jobName", "job1"))));

        when(getSpec2.uri(anyString())).thenReturn(headersSpec2);
        when(headersSpec2.retrieve()).thenReturn(responseSpec2);
        when(responseSpec2.body(eq(List.class)))
                .thenReturn(List.of(new java.util.HashMap<>(Map.of("jobName", "job2"))));

        ResponseEntity<List<Map<String, Object>>> response = controller.getAllSchedules();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldHandleNodeFailureGracefully() {
        when(discoveryService.getNodes()).thenReturn(List.of("http://bad-node:8080"));

        RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);

        when(restClient.get()).thenReturn(getSpec);
        when(getSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenThrow(new RuntimeException("Connection refused"));

        ResponseEntity<List<Map<String, Object>>> response = controller.getAllSchedules();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void shouldRejectCreateWithBlankNodeUrl() {
        Map<String, Object> body = Map.of("jobName", "test", "cronExpression", "0 0 * * * *", "nodeUrl", "  ");

        ResponseEntity<Map<String, Object>> response = controller.createSchedule(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldDeleteScheduleFromNode() {
        RestClient.RequestHeadersUriSpec deleteSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.delete()).thenReturn(deleteSpec);
        when(deleteSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(eq(Map.class))).thenReturn(Map.of("deleted", "test-job"));

        ResponseEntity<Map<String, Object>> response =
                controller.deleteSchedule("test-job", "http://node1:8080");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldHandleDeleteFailure() {
        RestClient.RequestHeadersUriSpec deleteSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);

        when(restClient.delete()).thenReturn(deleteSpec);
        when(deleteSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenThrow(new RuntimeException("Connection refused"));

        ResponseEntity<Map<String, Object>> response =
                controller.deleteSchedule("test-job", "http://bad-node:8080");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldCreateScheduleOnNode() {
        RestClient.RequestBodyUriSpec postSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(postSpec);
        when(postSpec.uri(anyString())).thenReturn(postSpec);
        when(postSpec.body(any(Object.class))).thenReturn(postSpec);
        when(postSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(eq(Map.class)))
                .thenReturn(new java.util.HashMap<>(Map.of("jobName", "new-job", "cronExpression", "0 0 * * * *")));

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("jobName", "new-job");
        body.put("cronExpression", "0 0 * * * *");
        body.put("nodeUrl", "http://node1:8080");

        ResponseEntity<Map<String, Object>> response = controller.createSchedule(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("jobName", "new-job");
        assertThat(response.getBody()).containsEntry("nodeUrl", "http://node1:8080");
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldHandleCreateScheduleFailure() {
        RestClient.RequestBodyUriSpec postSpec = mock(RestClient.RequestBodyUriSpec.class);

        when(restClient.post()).thenReturn(postSpec);
        when(postSpec.uri(anyString())).thenReturn(postSpec);
        when(postSpec.body(any(Object.class))).thenReturn(postSpec);
        when(postSpec.retrieve()).thenThrow(new RuntimeException("Connection refused"));

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("jobName", "new-job");
        body.put("cronExpression", "0 0 * * * *");
        body.put("nodeUrl", "http://bad-node:8080");

        ResponseEntity<Map<String, Object>> response = controller.createSchedule(body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldGetUpcomingTriggersFromNodes() {
        when(discoveryService.getNodes()).thenReturn(List.of("http://node1:8080"));

        RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(getSpec);
        when(getSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(eq(List.class)))
                .thenReturn(List.of(new java.util.HashMap<>(Map.of("jobName", "hourly-job"))));

        ResponseEntity<List<Map<String, Object>>> response = controller.getUpcomingTriggers(5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0)).containsEntry("nodeUrl", "http://node1:8080");
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldHandleUpcomingTriggersNodeFailure() {
        when(discoveryService.getNodes()).thenReturn(List.of("http://bad-node:8080"));

        RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);

        when(restClient.get()).thenReturn(getSpec);
        when(getSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenThrow(new RuntimeException("Connection refused"));

        ResponseEntity<List<Map<String, Object>>> response = controller.getUpcomingTriggers(5);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldGetRegisteredJobsFromNodes() {
        when(discoveryService.getNodes()).thenReturn(List.of("http://node1:8080"));

        RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(getSpec);
        when(getSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(eq(List.class)))
                .thenReturn(List.of(new java.util.HashMap<>(Map.of("name", "my-job"))));

        ResponseEntity<List<Map<String, Object>>> response = controller.getRegisteredJobs();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0)).containsEntry("nodeUrl", "http://node1:8080");
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldHandleRegisteredJobsNodeFailure() {
        when(discoveryService.getNodes()).thenReturn(List.of("http://bad-node:8080"));

        RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);

        when(restClient.get()).thenReturn(getSpec);
        when(getSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenThrow(new RuntimeException("Connection refused"));

        ResponseEntity<List<Map<String, Object>>> response = controller.getRegisteredJobs();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldDeleteScheduleWithNullBody() {
        RestClient.RequestHeadersUriSpec deleteSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.delete()).thenReturn(deleteSpec);
        when(deleteSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(eq(Map.class))).thenReturn(null);

        ResponseEntity<Map<String, Object>> response =
                controller.deleteSchedule("test-job", "http://node1:8080");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("deleted", "test-job");
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldHandleNullScheduleResponseFromNode() {
        when(discoveryService.getNodes()).thenReturn(List.of("http://node1:8080"));

        RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(getSpec);
        when(getSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(eq(List.class))).thenReturn(null);

        ResponseEntity<List<Map<String, Object>>> response = controller.getAllSchedules();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }
}
