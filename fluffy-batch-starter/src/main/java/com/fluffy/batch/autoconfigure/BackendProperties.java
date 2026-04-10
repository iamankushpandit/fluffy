package com.fluffy.batch.autoconfigure;

import com.fluffy.batch.backend.BackendType;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for selecting and configuring the Fluffy Batch backend.
 *
 * <p>The backend type determines how job queue state and concurrency coordination
 * are managed. Available modes:</p>
 * <ul>
 *   <li>{@code h2} — In-memory, single-node (default)</li>
 *   <li>{@code database} — Persistent, multi-node via shared database</li>
 *   <li>{@code kafka} — Distributed, multi-node via Kafka with database coordination</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "fluffy.batch.backend")
public class BackendProperties {

    /** The active backend type. Defaults to H2 (in-memory). */
    private BackendType type = BackendType.H2;

    private final KafkaBackendProperties kafka = new KafkaBackendProperties();

    public BackendType getType() {
        return type;
    }

    public void setType(BackendType type) {
        this.type = type;
    }

    public KafkaBackendProperties getKafka() {
        return kafka;
    }

    /**
     * Kafka-specific backend configuration.
     */
    public static class KafkaBackendProperties {
        private String bootstrapServers = "localhost:9092";
        private String topic = "fluffy-jobs";
        private String groupId = "fluffy-batch";

        public String getBootstrapServers() {
            return bootstrapServers;
        }

        public void setBootstrapServers(String bootstrapServers) {
            this.bootstrapServers = bootstrapServers;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }
    }
}
