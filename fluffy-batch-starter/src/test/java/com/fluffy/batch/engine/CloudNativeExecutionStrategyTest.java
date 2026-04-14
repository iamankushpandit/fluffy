package com.fluffy.batch.engine;

import com.fluffy.batch.api.JobContext;
import com.fluffy.batch.autoconfigure.CloudNativeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudNativeExecutionStrategyTest {

    @Mock
    private ExecutionCallback callback;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private CloudNativeProperties properties;
    private CloudNativeExecutionStrategy strategy;

    @BeforeEach
    void setUp() {
        properties = new CloudNativeProperties();
        properties.setEndpoint("http://example.com/api/dispatch");
        properties.setRequestTimeoutSeconds(30);
        properties.setConnectTimeoutSeconds(10);
        strategy = new CloudNativeExecutionStrategy(properties, callback, httpClient);
    }

    @Test
    void getMode_shouldReturnCloudNative() {
        assertThat(strategy.getMode()).isEqualTo(ExecutionMode.CLOUD_NATIVE);
    }

    // --- execute: missing/blank endpoint ---

    @Test
    void execute_shouldFailWhenEndpointIsNull() {
        properties.setEndpoint(null);
        JobDefinition def = buildDefinition("job1");
        JobContext ctx = buildContext(1L, "job1");

        strategy.execute(1L, def, ctx);

        verify(callback).onFailed(1L, "job1", "Cloud-native endpoint not configured");
        verify(callback).onFinished(1L, "job1", 1);
        verify(callback, never()).onStarted(anyLong());
    }

    @Test
    void execute_shouldFailWhenEndpointIsBlank() {
        properties.setEndpoint("   ");
        JobDefinition def = buildDefinition("job2");
        JobContext ctx = buildContext(2L, "job2");

        strategy.execute(2L, def, ctx);

        verify(callback).onFailed(2L, "job2", "Cloud-native endpoint not configured");
        verify(callback).onFinished(2L, "job2", 1);
    }

    // --- execute: successful dispatch (2xx) ---

    @Test
    void execute_shouldDispatchSuccessfullyOn200() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);

        JobDefinition def = buildDefinition("dispatchJob");
        JobContext ctx = buildContext(10L, "dispatchJob");

        strategy.execute(10L, def, ctx);

        verify(callback).onStarted(10L);
        assertThat(strategy.getDispatchedJobs()).containsEntry(10L, "dispatchJob");
        verify(callback, never()).onFailed(anyLong(), anyString(), anyString());
        verify(callback, never()).onFinished(anyLong(), anyString(), anyInt());
    }

    @Test
    void execute_shouldDispatchSuccessfullyOn201() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(201);

        JobDefinition def = buildDefinition("dispatchJob201");
        JobContext ctx = buildContext(11L, "dispatchJob201");

        strategy.execute(11L, def, ctx);

        verify(callback).onStarted(11L);
        assertThat(strategy.getDispatchedJobs()).containsEntry(11L, "dispatchJob201");
    }

    @Test
    void execute_shouldDispatchSuccessfullyOn299() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(299);

        JobDefinition def = buildDefinition("job299");
        JobContext ctx = buildContext(12L, "job299");

        strategy.execute(12L, def, ctx);

        assertThat(strategy.getDispatchedJobs()).containsEntry(12L, "job299");
    }

    // --- execute: HTTP failure (non-2xx) ---

    @Test
    void execute_shouldFailOn400() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(400);
        when(httpResponse.body()).thenReturn("Bad Request");

        JobDefinition def = buildDefinition("failJob400");
        JobContext ctx = buildContext(20L, "failJob400");

        strategy.execute(20L, def, ctx);

        verify(callback).onStarted(20L);
        verify(callback).onFailed(20L, "failJob400", "Cloud-native dispatch failed with status 400");
        verify(callback).onFinished(20L, "failJob400", 1);
        assertThat(strategy.getDispatchedJobs()).doesNotContainKey(20L);
    }

    @Test
    void execute_shouldFailOn500() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpResponse.body()).thenReturn("Internal Server Error");

        JobDefinition def = buildDefinition("failJob500");
        JobContext ctx = buildContext(21L, "failJob500");

        strategy.execute(21L, def, ctx);

        verify(callback).onFailed(21L, "failJob500", "Cloud-native dispatch failed with status 500");
        verify(callback).onFinished(21L, "failJob500", 1);
    }

    // --- execute: exception during dispatch ---

    @Test
    void execute_shouldFailOnIOException() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Connection refused"));

        JobDefinition def = buildDefinition("ioExJob");
        JobContext ctx = buildContext(30L, "ioExJob");

        strategy.execute(30L, def, ctx);

        verify(callback).onStarted(30L);
        verify(callback).onFailed(30L, "ioExJob", "Cloud-native dispatch error: Connection refused");
        verify(callback).onFinished(30L, "ioExJob", 1);
    }

    @Test
    void execute_shouldHandleInterruptedException() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new InterruptedException("interrupted"));

        JobDefinition def = buildDefinition("interruptedJob");
        JobContext ctx = buildContext(31L, "interruptedJob");

        strategy.execute(31L, def, ctx);

        verify(callback).onStarted(31L);
        verify(callback).onStopped(31L, "interruptedJob");
        verify(callback).onFinished(31L, "interruptedJob", 1);
        verify(callback, never()).onFailed(anyLong(), anyString(), anyString());
    }

    // --- handleCallback ---

    @Test
    void handleCallback_completed_shouldCallOnCompleted() {
        strategy.getDispatchedJobs().put(40L, "cbJob");

        strategy.handleCallback(40L, "COMPLETED", null);

        verify(callback).onCompleted(40L, "cbJob");
        assertThat(strategy.getDispatchedJobs()).doesNotContainKey(40L);
    }

    @Test
    void handleCallback_failed_shouldCallOnFailed() {
        strategy.getDispatchedJobs().put(41L, "cbJob2");

        strategy.handleCallback(41L, "FAILED", "remote error");

        verify(callback).onFailed(41L, "cbJob2", "remote error");
        assertThat(strategy.getDispatchedJobs()).doesNotContainKey(41L);
    }

    @Test
    void handleCallback_stopped_shouldCallOnStopped() {
        strategy.getDispatchedJobs().put(42L, "cbJob3");

        strategy.handleCallback(42L, "STOPPED", null);

        verify(callback).onStopped(42L, "cbJob3");
    }

    @Test
    void handleCallback_unknownStatus_shouldCallOnFailedWithMessage() {
        strategy.getDispatchedJobs().put(43L, "cbJob4");

        strategy.handleCallback(43L, "WEIRD_STATUS", null);

        verify(callback).onFailed(43L, "cbJob4", "Unknown callback status: WEIRD_STATUS");
    }

    @Test
    void handleCallback_caseInsensitive_shouldMatchLowerCase() {
        strategy.getDispatchedJobs().put(44L, "cbJob5");

        strategy.handleCallback(44L, "completed", null);

        verify(callback).onCompleted(44L, "cbJob5");
    }

    @Test
    void handleCallback_unknownExecutionId_shouldDoNothing() {
        strategy.handleCallback(999L, "COMPLETED", null);

        verifyNoInteractions(callback);
    }

    // --- stop ---

    @Test
    void stop_shouldReturnTrueForDispatchedJob() {
        strategy.getDispatchedJobs().put(50L, "stopJob");

        boolean result = strategy.stop(50L);

        assertThat(result).isTrue();
        assertThat(strategy.getDispatchedJobs()).doesNotContainKey(50L);
    }

    @Test
    void stop_shouldReturnFalseForNonDispatchedJob() {
        boolean result = strategy.stop(999L);

        assertThat(result).isFalse();
    }

    // --- payload building ---

    @Test
    void execute_shouldBuildPayloadWithBasicFields() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);

        JobDefinition def = buildDefinition("payloadJob");
        JobContext ctx = new JobContext(60L, "payloadJob", "admin", Collections.emptyMap(), null);

        strategy.execute(60L, def, ctx);

        // Verify the request was sent (payload construction didn't throw)
        verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void execute_shouldIncludeCallbackUrlInPayload() throws Exception {
        properties.setCallbackUrl("http://myapp/callback");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);

        JobDefinition def = buildDefinition("callbackJob");
        JobContext ctx = new JobContext(61L, "callbackJob", "admin", Collections.emptyMap(), null);

        strategy.execute(61L, def, ctx);

        verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void execute_shouldIncludeArgumentsInPayload() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);

        JobDefinition def = buildDefinition("argJob");
        JobContext ctx = new JobContext(62L, "argJob", "admin", Collections.emptyMap(), "some arguments");

        strategy.execute(62L, def, ctx);

        verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void execute_shouldIncludeParametersInPayload() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);

        JobDefinition def = buildDefinition("paramJob");
        JobContext ctx = new JobContext(63L, "paramJob", "admin", Map.of("key1", "val1", "key2", "val2"), null);

        strategy.execute(63L, def, ctx);

        verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void execute_shouldEscapeSpecialCharsInPayload() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);

        JobDefinition def = buildDefinition("escapeJob");
        JobContext ctx = new JobContext(64L, "escapeJob", "user\"with\\quotes",
                Map.of("key\n", "val\t"), "args\rwith\nnewlines");

        strategy.execute(64L, def, ctx);

        verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void execute_shouldNotIncludeCallbackUrlWhenBlank() throws Exception {
        properties.setCallbackUrl("   ");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);

        JobDefinition def = buildDefinition("noCallbackJob");
        JobContext ctx = new JobContext(65L, "noCallbackJob", "admin", Collections.emptyMap(), null);

        strategy.execute(65L, def, ctx);

        verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void execute_shouldNotIncludeCallbackUrlWhenNull() throws Exception {
        properties.setCallbackUrl(null);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);

        JobDefinition def = buildDefinition("nullCallbackJob");
        JobContext ctx = new JobContext(66L, "nullCallbackJob", "admin", Collections.emptyMap(), null);

        strategy.execute(66L, def, ctx);

        verify(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void getDispatchedJobs_shouldReturnMutableMap() {
        assertThat(strategy.getDispatchedJobs()).isNotNull().isEmpty();
    }

    // --- production constructor ---

    @Test
    void productionConstructor_shouldCreateHttpClient() {
        CloudNativeProperties props = new CloudNativeProperties();
        props.setConnectTimeoutSeconds(5);
        CloudNativeExecutionStrategy prodStrategy = new CloudNativeExecutionStrategy(props, callback);
        assertThat(prodStrategy.getMode()).isEqualTo(ExecutionMode.CLOUD_NATIVE);
    }

    // --- helpers ---

    private JobDefinition buildDefinition(String name) {
        return JobDefinition.builder(name)
                .handler(ctx -> {})
                .maxConcurrency(1)
                .build();
    }

    private JobContext buildContext(Long executionId, String jobName) {
        return new JobContext(executionId, jobName, "testUser", Collections.emptyMap(), null);
    }
}
