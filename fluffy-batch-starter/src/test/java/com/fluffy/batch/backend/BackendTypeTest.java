package com.fluffy.batch.backend;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class BackendTypeTest {

    @Test
    void shouldHaveThreeBackendTypes() {
        assertThat(BackendType.values()).hasSize(3);
        assertThat(BackendType.valueOf("H2")).isEqualTo(BackendType.H2);
        assertThat(BackendType.valueOf("DATABASE")).isEqualTo(BackendType.DATABASE);
        assertThat(BackendType.valueOf("KAFKA")).isEqualTo(BackendType.KAFKA);
    }
}
