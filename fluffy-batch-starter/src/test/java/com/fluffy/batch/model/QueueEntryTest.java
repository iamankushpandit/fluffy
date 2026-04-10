package com.fluffy.batch.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class QueueEntryTest {

    @Test
    void shouldCreateWithConstructor() {
        QueueEntry entry = new QueueEntry(1L, "testJob", 5);
        assertThat(entry.getExecutionId()).isEqualTo(1L);
        assertThat(entry.getJobName()).isEqualTo("testJob");
        assertThat(entry.getPosition()).isEqualTo(5);
        assertThat(entry.getStatus()).isEqualTo("QUEUED");
        assertThat(entry.getCreatedAt()).isNotNull();
        assertThat(entry.getClaimedBy()).isNull();
        assertThat(entry.getClaimedAt()).isNull();
    }

    @Test
    void shouldCreateWithDefaultConstructor() {
        QueueEntry entry = new QueueEntry();
        assertThat(entry.getId()).isNull();
        assertThat(entry.getExecutionId()).isNull();
    }

    @Test
    void shouldSupportAllSetters() {
        QueueEntry entry = new QueueEntry();
        Instant now = Instant.now();

        entry.setId(10L);
        entry.setExecutionId(20L);
        entry.setJobName("myJob");
        entry.setPosition(3);
        entry.setStatus("CLAIMED");
        entry.setClaimedBy("node-1");
        entry.setClaimedAt(now);
        entry.setCreatedAt(now);

        assertThat(entry.getId()).isEqualTo(10L);
        assertThat(entry.getExecutionId()).isEqualTo(20L);
        assertThat(entry.getJobName()).isEqualTo("myJob");
        assertThat(entry.getPosition()).isEqualTo(3);
        assertThat(entry.getStatus()).isEqualTo("CLAIMED");
        assertThat(entry.getClaimedBy()).isEqualTo("node-1");
        assertThat(entry.getClaimedAt()).isEqualTo(now);
        assertThat(entry.getCreatedAt()).isEqualTo(now);
    }
}
