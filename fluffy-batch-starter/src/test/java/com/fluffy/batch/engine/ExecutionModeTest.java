package com.fluffy.batch.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ExecutionModeTest {

    @Test
    void shouldContainLocalAndCloudNativeValues() {
        ExecutionMode[] values = ExecutionMode.values();
        assertThat(values).containsExactly(ExecutionMode.LOCAL, ExecutionMode.CLOUD_NATIVE);
    }

    @Test
    void shouldResolveLocalFromValueOf() {
        assertThat(ExecutionMode.valueOf("LOCAL")).isEqualTo(ExecutionMode.LOCAL);
    }

    @Test
    void shouldResolveCloudNativeFromValueOf() {
        assertThat(ExecutionMode.valueOf("CLOUD_NATIVE")).isEqualTo(ExecutionMode.CLOUD_NATIVE);
    }

    @Test
    void shouldThrowForInvalidValueOf() {
        assertThatThrownBy(() -> ExecutionMode.valueOf("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldHaveCorrectOrdinals() {
        assertThat(ExecutionMode.LOCAL.ordinal()).isEqualTo(0);
        assertThat(ExecutionMode.CLOUD_NATIVE.ordinal()).isEqualTo(1);
    }

    @Test
    void shouldHaveCorrectName() {
        assertThat(ExecutionMode.LOCAL.name()).isEqualTo("LOCAL");
        assertThat(ExecutionMode.CLOUD_NATIVE.name()).isEqualTo("CLOUD_NATIVE");
    }
}
