package com.fluffy.batch.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NodeIdResolverTest {

    @Test
    void shouldReturnNonNullNodeId() {
        assertThat(NodeIdResolver.getNodeId()).isNotNull();
    }

    @Test
    void shouldReturnNonBlankNodeId() {
        assertThat(NodeIdResolver.getNodeId()).isNotBlank();
    }

    @Test
    void shouldReturnConsistentNodeId() {
        String first = NodeIdResolver.getNodeId();
        String second = NodeIdResolver.getNodeId();
        assertThat(first).isEqualTo(second);
    }
}
