package com.fluffy.batch.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JobNotFoundExceptionTest {

    @Test
    void shouldCarryMessage() {
        JobNotFoundException ex = new JobNotFoundException("not found: 42");
        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("not found: 42");
    }
}
