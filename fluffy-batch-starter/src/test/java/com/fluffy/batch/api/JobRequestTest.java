package com.fluffy.batch.api;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class JobRequestTest {

    @Test
    void shouldCreateWithNoArgConstructor() {
        JobRequest request = new JobRequest();

        assertThat(request.parameters()).isNotNull().isEmpty();
        assertThat(request.arguments()).isNull();
        assertThat(request.requestedBy()).isNull();
    }

    @Test
    void shouldCreateWithAllArgs() {
        Map<String, String> params = Map.of("key1", "value1");
        JobRequest request = new JobRequest(params, "args", "user1");

        assertThat(request.parameters()).containsEntry("key1", "value1");
        assertThat(request.arguments()).isEqualTo("args");
        assertThat(request.requestedBy()).isEqualTo("user1");
    }

    @Test
    void shouldStoreUnmodifiableCopyOfParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("key1", "value1");
        JobRequest request = new JobRequest(params, null, null);

        assertThatThrownBy(() -> request.parameters().put("key2", "value2"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldHandleNullParameters() {
        JobRequest request = new JobRequest(null, null, null);

        assertThat(request.parameters()).isNotNull().isEmpty();
    }

    @Test
    void shouldCreateWithRequestedByUsingWith() {
        JobRequest original = new JobRequest(Map.of("k", "v"), "args", null);
        JobRequest updated = original.withRequestedBy("new-user");

        assertThat(updated.requestedBy()).isEqualTo("new-user");
        assertThat(updated.parameters()).containsEntry("k", "v");
        assertThat(updated.arguments()).isEqualTo("args");
    }

    @Test
    void shouldOverrideRequestedByUsingWith() {
        JobRequest original = new JobRequest(null, null, "old-user");
        JobRequest updated = original.withRequestedBy("new-user");

        assertThat(updated.requestedBy()).isEqualTo("new-user");
        assertThat(original.requestedBy()).isEqualTo("old-user");
    }
}
