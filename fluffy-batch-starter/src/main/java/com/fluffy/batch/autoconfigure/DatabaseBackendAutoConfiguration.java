package com.fluffy.batch.autoconfigure;

import com.fluffy.batch.backend.BackendType;
import com.fluffy.batch.backend.CoordinationBackend;
import com.fluffy.batch.backend.DbCoordinationBackend;
import com.fluffy.batch.backend.DbQueueBackend;
import com.fluffy.batch.backend.QueueBackend;
import com.fluffy.batch.persistence.JobExecutionRepository;
import com.fluffy.batch.persistence.QueueEntryRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.net.InetAddress;

/**
 * Auto-configuration that activates the database-backed queue and coordination
 * backends when {@code fluffy.batch.backend.type=database}.
 */
@AutoConfiguration(before = BatchJobAutoConfiguration.class)
@ConditionalOnProperty(name = "fluffy.batch.backend.type", havingValue = "database")
public class DatabaseBackendAutoConfiguration {

    @Bean
    public QueueBackend queueBackend(QueueEntryRepository queueEntryRepository) {
        return new DbQueueBackend(queueEntryRepository, resolveNodeId());
    }

    @Bean
    public CoordinationBackend coordinationBackend(JobExecutionRepository executionRepository) {
        return new DbCoordinationBackend(executionRepository);
    }

    private static String resolveNodeId() {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            return hostname != null ? hostname : "node-" + ProcessHandle.current().pid();
        } catch (Exception e) {
            return "node-" + ProcessHandle.current().pid();
        }
    }
}
