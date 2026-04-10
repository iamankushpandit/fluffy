package com.fluffy.batch.autoconfigure;

import com.fluffy.batch.backend.BackendType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class BackendPropertiesTest {

    @Test
    void shouldHaveSensibleDefaults() {
        BackendProperties props = new BackendProperties();
        assertThat(props.getType()).isEqualTo(BackendType.H2);
        assertThat(props.getKafka()).isNotNull();
        assertThat(props.getKafka().getBootstrapServers()).isEqualTo("localhost:9092");
        assertThat(props.getKafka().getTopic()).isEqualTo("fluffy-jobs");
        assertThat(props.getKafka().getGroupId()).isEqualTo("fluffy-batch");
    }

    @Test
    void shouldAllowSettingType() {
        BackendProperties props = new BackendProperties();
        props.setType(BackendType.DATABASE);
        assertThat(props.getType()).isEqualTo(BackendType.DATABASE);
    }

    @Test
    void shouldAllowSettingKafkaProperties() {
        BackendProperties props = new BackendProperties();
        props.getKafka().setBootstrapServers("kafka:9093");
        props.getKafka().setTopic("custom-topic");
        props.getKafka().setGroupId("custom-group");
        assertThat(props.getKafka().getBootstrapServers()).isEqualTo("kafka:9093");
        assertThat(props.getKafka().getTopic()).isEqualTo("custom-topic");
        assertThat(props.getKafka().getGroupId()).isEqualTo("custom-group");
    }
}
