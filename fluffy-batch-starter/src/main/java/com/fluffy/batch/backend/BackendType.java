package com.fluffy.batch.backend;

/**
 * Supported backend types for queue and coordination infrastructure.
 */
public enum BackendType {

    /** In-memory H2 mode — ideal for local development and single-node use. */
    H2,

    /** Database-backed mode — uses a shared relational database for persistent queue and coordination. */
    DATABASE,

    /** Kafka-backed mode — uses Kafka for distributed queue with database coordination. */
    KAFKA
}
