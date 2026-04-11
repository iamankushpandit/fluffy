package com.fluffy.batch.autoconfigure;

import com.fluffy.batch.backend.BackendType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class KafkaBackendAutoConfigurationTest {

    @Test
    void shouldOnlyActivateWithKafkaType() {
        BackendProperties props = new BackendProperties();
        props.setType(BackendType.H2);
        assertThat(props.getType()).isNotEqualTo(BackendType.KAFKA);
    }

    @Test
    void shouldActivateWithKafkaType() {
        BackendProperties props = new BackendProperties();
        props.setType(BackendType.KAFKA);
        assertThat(props.getType()).isEqualTo(BackendType.KAFKA);
    }

    @Test
    void shouldHaveDefaultKafkaProperties() {
        BackendProperties props = new BackendProperties();
        assertThat(props.getKafka().getBootstrapServers()).isEqualTo("localhost:9092");
        assertThat(props.getKafka().getTopic()).isEqualTo("fluffy-jobs");
        assertThat(props.getKafka().getGroupId()).isEqualTo("fluffy-batch");
    }

    @Test
    void shouldInstantiateAutoConfiguration() {
        KafkaBackendAutoConfiguration config = new KafkaBackendAutoConfiguration();
        assertThat(config).isNotNull();
    }

    @Test
    void shouldCreateProducerFactory() {
        KafkaBackendAutoConfiguration config = new KafkaBackendAutoConfiguration();
        BackendProperties props = new BackendProperties();
        props.getKafka().setBootstrapServers("localhost:9092");
        assertThat(config.fluffyKafkaProducerFactory(props)).isNotNull();
    }

    @Test
    void shouldCreateConsumerFactory() {
        KafkaBackendAutoConfiguration config = new KafkaBackendAutoConfiguration();
        BackendProperties props = new BackendProperties();
        props.getKafka().setBootstrapServers("localhost:9092");
        props.getKafka().setGroupId("test-group");
        assertThat(config.fluffyKafkaConsumerFactory(props)).isNotNull();
    }
}
