package com.fluffy.batch.web;

import com.fluffy.batch.engine.CloudNativeExecutionStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudNativeCallbackControllerTest {

    @Mock
    private CloudNativeExecutionStrategy strategy;

    private CloudNativeCallbackController controller;

    @BeforeEach
    void setUp() {
        controller = new CloudNativeCallbackController(strategy);
    }

    @Test
    void handleCallback_shouldReturn200OnValidRequest() {
        Map<String, String> body = new HashMap<>();
        body.put("executionId", "123");
        body.put("status", "COMPLETED");

        ResponseEntity<Map<String, String>> response = controller.handleCallback(body);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("status", "accepted");
        verify(strategy).handleCallback(123L, "COMPLETED", null);
    }

    @Test
    void handleCallback_shouldPassErrorMessage() {
        Map<String, String> body = new HashMap<>();
        body.put("executionId", "456");
        body.put("status", "FAILED");
        body.put("errorMessage", "Something broke");

        ResponseEntity<Map<String, String>> response = controller.handleCallback(body);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(strategy).handleCallback(456L, "FAILED", "Something broke");
    }

    @Test
    void handleCallback_shouldReturn400WhenExecutionIdMissing() {
        Map<String, String> body = new HashMap<>();
        body.put("status", "COMPLETED");

        ResponseEntity<Map<String, String>> response = controller.handleCallback(body);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("error", "executionId and status are required");
        verifyNoInteractions(strategy);
    }

    @Test
    void handleCallback_shouldReturn400WhenStatusMissing() {
        Map<String, String> body = new HashMap<>();
        body.put("executionId", "123");

        ResponseEntity<Map<String, String>> response = controller.handleCallback(body);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("error", "executionId and status are required");
        verifyNoInteractions(strategy);
    }

    @Test
    void handleCallback_shouldReturn400WhenBothMissing() {
        Map<String, String> body = new HashMap<>();

        ResponseEntity<Map<String, String>> response = controller.handleCallback(body);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("error", "executionId and status are required");
    }

    @Test
    void handleCallback_shouldReturn400WhenExecutionIdNotANumber() {
        Map<String, String> body = new HashMap<>();
        body.put("executionId", "not-a-number");
        body.put("status", "COMPLETED");

        ResponseEntity<Map<String, String>> response = controller.handleCallback(body);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("error", "executionId must be a number");
        verifyNoInteractions(strategy);
    }

    @Test
    void handleCallback_shouldHandleStoppedStatus() {
        Map<String, String> body = new HashMap<>();
        body.put("executionId", "789");
        body.put("status", "STOPPED");

        ResponseEntity<Map<String, String>> response = controller.handleCallback(body);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(strategy).handleCallback(789L, "STOPPED", null);
    }

    @Test
    void handleCallback_shouldHandleNullErrorMessage() {
        Map<String, String> body = new HashMap<>();
        body.put("executionId", "100");
        body.put("status", "COMPLETED");
        // errorMessage not in map → null

        ResponseEntity<Map<String, String>> response = controller.handleCallback(body);

        verify(strategy).handleCallback(100L, "COMPLETED", null);
    }
}
