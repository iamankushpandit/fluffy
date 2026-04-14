package com.fluffy.batch.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JobDefinitionTest {

    @Test
    void shouldBuildWithDefaults() {
        JobDefinition def = JobDefinition.builder("test-job")
                .handler(ctx -> {})
                .build();

        assertThat(def.name()).isEqualTo("test-job");
        assertThat(def.description()).isEmpty();
        assertThat(def.maxConcurrency()).isEqualTo(1);
        assertThat(def.async()).isTrue();
        assertThat(def.timeoutSeconds()).isZero();
        assertThat(def.requiredParams()).isEmpty();
        assertThat(def.handler()).isNotNull();
    }

    @Test
    void shouldBuildWithAllFields() {
        JobDefinition def = JobDefinition.builder("my-job")
                .description("A test job")
                .maxConcurrency(5)
                .async(false)
                .timeoutSeconds(120)
                .requiredParams("param1", "param2")
                .handler(ctx -> {})
                .build();

        assertThat(def.name()).isEqualTo("my-job");
        assertThat(def.description()).isEqualTo("A test job");
        assertThat(def.maxConcurrency()).isEqualTo(5);
        assertThat(def.async()).isFalse();
        assertThat(def.timeoutSeconds()).isEqualTo(120);
        assertThat(def.requiredParams()).containsExactly("param1", "param2");
    }

    @Test
    void shouldThrowForBlankName() {
        assertThatThrownBy(() -> JobDefinition.builder("")
                .handler(ctx -> {})
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name must not be blank");
    }

    @Test
    void shouldThrowForNullName() {
        assertThatThrownBy(() -> JobDefinition.builder(null)
                .handler(ctx -> {})
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name must not be blank");
    }

    @Test
    void shouldThrowForNullHandler() {
        assertThatThrownBy(() -> JobDefinition.builder("test-job")
                .handler(null)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("handler must not be null");
    }

    @Test
    void shouldDefaultNullRequiredParamsToEmpty() {
        JobDefinition def = new JobDefinition("test", "", 1, true, 0, null, ctx -> {}, null);
        assertThat(def.requiredParams()).isNotNull().isEmpty();
        assertThat(def.executionMode()).isEqualTo(ExecutionMode.LOCAL);
    }

    @Test
    void shouldCreateViaRecordConstructor() {
        String[] params = {"p1"};
        JobDefinition def = new JobDefinition("direct-job", "desc", 2, false, 30, params, ctx -> {}, ExecutionMode.CLOUD_NATIVE);

        assertThat(def.name()).isEqualTo("direct-job");
        assertThat(def.description()).isEqualTo("desc");
        assertThat(def.maxConcurrency()).isEqualTo(2);
        assertThat(def.async()).isFalse();
        assertThat(def.timeoutSeconds()).isEqualTo(30);
        assertThat(def.requiredParams()).containsExactly("p1");
        assertThat(def.executionMode()).isEqualTo(ExecutionMode.CLOUD_NATIVE);
    }

    @Test
    void shouldBuildWithExecutionMode() {
        JobDefinition def = JobDefinition.builder("cloud-job")
                .handler(ctx -> {})
                .executionMode(ExecutionMode.CLOUD_NATIVE)
                .build();

        assertThat(def.executionMode()).isEqualTo(ExecutionMode.CLOUD_NATIVE);
    }

    @Test
    void shouldDefaultExecutionModeToLocal() {
        JobDefinition def = JobDefinition.builder("local-job")
                .handler(ctx -> {})
                .build();

        assertThat(def.executionMode()).isEqualTo(ExecutionMode.LOCAL);
    }
}
