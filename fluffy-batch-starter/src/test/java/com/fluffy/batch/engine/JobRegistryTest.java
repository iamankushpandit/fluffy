package com.fluffy.batch.engine;

import com.fluffy.batch.api.JobContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = com.fluffy.batch.TestBatchApplication.class)
class JobRegistryTest {

    @Autowired
    private JobRegistry jobRegistry;

    private JobDefinition testDef;

    @BeforeEach
    void setUp() {
        String uniqueName = "test-job-" + System.nanoTime();
        testDef = JobDefinition.builder(uniqueName)
                .description("Test job")
                .handler(ctx -> {})
                .build();
    }

    @Test
    void shouldRegisterJob() {
        jobRegistry.register(testDef);
        assertThat(jobRegistry.exists(testDef.getName())).isTrue();
    }

    @Test
    void shouldRetrieveRegisteredJob() {
        jobRegistry.register(testDef);
        JobDefinition retrieved = jobRegistry.get(testDef.getName());
        assertThat(retrieved.getName()).isEqualTo(testDef.getName());
        assertThat(retrieved.getDescription()).isEqualTo("Test job");
    }

    @Test
    void shouldThrowWhenDuplicateRegistration() {
        jobRegistry.register(testDef);
        assertThatThrownBy(() -> jobRegistry.register(testDef))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void shouldThrowWhenJobNotFound() {
        assertThatThrownBy(() -> jobRegistry.get("nonexistent-job-xyz"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("nonexistent-job-xyz");
    }

    @Test
    void shouldReturnFalseForNonExistentJob() {
        assertThat(jobRegistry.exists("nonexistent-xyz")).isFalse();
    }

    @Test
    void shouldListAllJobs() {
        jobRegistry.register(testDef);
        assertThat(jobRegistry.getAll()).isNotNull();
        assertThat(jobRegistry.getAll()).anyMatch(d -> d.getName().equals(testDef.getName()));
    }
}
