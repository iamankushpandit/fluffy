package com.fluffy.batch.engine;

import com.fluffy.batch.annotation.BatchJob;
import com.fluffy.batch.api.JobContext;
import com.fluffy.batch.api.JobHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = com.fluffy.batch.TestBatchApplication.class)
@Import({JobRegistryAutoDiscoveryTest.TestJobConfig.class,
         JobRegistryAutoDiscoveryTest.InterfaceAnnotatedJobConfig.class})
class JobRegistryAutoDiscoveryTest {

    @Autowired
    private JobRegistry jobRegistry;

    // Direct class-level annotation
    @BatchJob(name = "auto-discovered-job", description = "A test auto-discovered job",
              maxConcurrency = 3, async = false, timeoutSeconds = 60,
              requiredParams = {"param1"})
    static class TestAutoDiscoveredJob implements JobHandler {
        @Override
        public void execute(JobContext context) throws Exception {
            // no-op for testing
        }
    }

    // Interface-level annotation (covers the fallback path in JobRegistry.afterPropertiesSet)
    @BatchJob(name = "iface-annotated-job", description = "Interface annotated",
              maxConcurrency = 2, async = true)
    interface AnnotatedJobInterface extends JobHandler {}

    static class InterfaceAnnotatedJobImpl implements AnnotatedJobInterface {
        @Override
        public void execute(JobContext context) throws Exception {
            // no-op
        }
    }

    @TestConfiguration
    static class TestJobConfig {
        @Bean
        public TestAutoDiscoveredJob testAutoDiscoveredJob() {
            return new TestAutoDiscoveredJob();
        }
    }

    @TestConfiguration
    static class InterfaceAnnotatedJobConfig {
        @Bean
        public InterfaceAnnotatedJobImpl interfaceAnnotatedJob() {
            return new InterfaceAnnotatedJobImpl();
        }
    }

    @Test
    void shouldAutoDiscoverBatchJobAnnotatedBeans() {
        assertThat(jobRegistry.exists("auto-discovered-job")).isTrue();

        JobDefinition def = jobRegistry.get("auto-discovered-job");
        assertThat(def.name()).isEqualTo("auto-discovered-job");
        assertThat(def.description()).isEqualTo("A test auto-discovered job");
        assertThat(def.maxConcurrency()).isEqualTo(3);
        assertThat(def.async()).isFalse();
        assertThat(def.timeoutSeconds()).isEqualTo(60);
        assertThat(def.requiredParams()).containsExactly("param1");
    }

    @Test
    void shouldAutoDiscoverJobAnnotatedOnInterface() {
        assertThat(jobRegistry.exists("iface-annotated-job")).isTrue();

        JobDefinition def = jobRegistry.get("iface-annotated-job");
        assertThat(def.name()).isEqualTo("iface-annotated-job");
        assertThat(def.description()).isEqualTo("Interface annotated");
        assertThat(def.maxConcurrency()).isEqualTo(2);
        assertThat(def.async()).isTrue();
    }
}
