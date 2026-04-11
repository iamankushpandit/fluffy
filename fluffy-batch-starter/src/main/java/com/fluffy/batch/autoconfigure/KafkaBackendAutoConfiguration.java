package com.fluffy.batch.autoconfigure;

import com.fluffy.batch.backend.CoordinationBackend;
import com.fluffy.batch.backend.DbCoordinationBackend;
import com.fluffy.batch.backend.KafkaQueueBackend;
import com.fluffy.batch.backend.QueueBackend;
import com.fluffy.batch.persistence.JobExecutionRepository;
import com.fluffy.batch.persistence.QueueEntryRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Auto-configuration that activates the Kafka-backed queue and database coordination
 * backends when {@code fluffy.batch.backend.type=kafka}.
 * <p>
 * Kafka is used for distributed message delivery while the database is used
 * for coordination state (concurrency tracking, execution ownership).
 * </p>
 */
@AutoConfiguration(before = BatchJobAutoConfiguration.class)
@ConditionalOnProperty(name = "fluffy.batch.backend.type", havingValue = "kafka")
@ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
public class KafkaBackendAutoConfiguration {

    @Bean
    public ProducerFactory<String, String> fluffyKafkaProducerFactory(BackendProperties properties) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getKafka().getBootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, String> fluffyKafkaTemplate(ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public ConsumerFactory<String, String> fluffyKafkaConsumerFactory(BackendProperties properties) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getKafka().getBootstrapServers());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, properties.getKafka().getGroupId());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public KafkaQueueBackend queueBackend(KafkaTemplate<String, String> fluffyKafkaTemplate,
                                          BackendProperties properties,
                                          QueueEntryRepository queueEntryRepository) {
        return new KafkaQueueBackend(fluffyKafkaTemplate, properties.getKafka().getTopic(), queueEntryRepository);
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, String> fluffyKafkaListenerContainer(
            ConsumerFactory<String, String> fluffyKafkaConsumerFactory,
            KafkaQueueBackend kafkaQueueBackend,
            BackendProperties properties) {
        ContainerProperties containerProperties = new ContainerProperties(properties.getKafka().getTopic());
        containerProperties.setMessageListener(kafkaQueueBackend);
        return new ConcurrentMessageListenerContainer<>(fluffyKafkaConsumerFactory, containerProperties);
    }

    @Bean
    public CoordinationBackend coordinationBackend(JobExecutionRepository executionRepository) {
        return new DbCoordinationBackend(executionRepository);
    }
}
