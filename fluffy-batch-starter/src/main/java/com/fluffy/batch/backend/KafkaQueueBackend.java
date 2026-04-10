package com.fluffy.batch.backend;

import com.fluffy.batch.model.QueueEntry;
import com.fluffy.batch.persistence.QueueEntryRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Kafka-backed queue implementation.
 * <p>
 * Job execution ids are published to a Kafka topic and consumed by this backend.
 * The consumed entries are buffered locally for polling by the JobLauncher.
 * A database-backed position tracker provides persistent queue metadata.
 * </p>
 */
public class KafkaQueueBackend implements QueueBackend, MessageListener<String, String> {

    private static final Logger log = LoggerFactory.getLogger(KafkaQueueBackend.class);
    private static final String QUEUED = "QUEUED";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;
    private final QueueEntryRepository queueEntryRepository;
    private final BlockingQueue<Long> localBuffer = new LinkedBlockingQueue<>();
    private final AtomicInteger positionCounter = new AtomicInteger(0);

    public KafkaQueueBackend(KafkaTemplate<String, String> kafkaTemplate,
                             String topic,
                             QueueEntryRepository queueEntryRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.queueEntryRepository = queueEntryRepository;
    }

    @Override
    @Transactional
    public void enqueue(Long executionId) {
        int position = positionCounter.incrementAndGet();
        QueueEntry entry = new QueueEntry(executionId, "", position);
        queueEntryRepository.save(entry);
        kafkaTemplate.send(new ProducerRecord<>(topic, executionId.toString(), executionId.toString()));
        log.debug("Published execution {} to Kafka topic {}", executionId, topic);
    }

    @Override
    public Long poll() {
        Long executionId = localBuffer.poll();
        if (executionId != null) {
            markClaimed(executionId);
            log.debug("Polled execution {} from Kafka buffer", executionId);
        }
        return executionId;
    }

    @Override
    @Transactional
    public boolean remove(Long executionId) {
        localBuffer.remove(executionId);
        int deleted = queueEntryRepository.deleteByExecutionIdAndStatus(executionId, QUEUED);
        if (deleted > 0) {
            log.debug("Removed execution {} from Kafka queue", executionId);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getPosition(Long executionId) {
        Optional<QueueEntry> entry = queueEntryRepository.findByExecutionIdAndStatus(executionId, QUEUED);
        return entry.map(QueueEntry::getPosition).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public int size() {
        return (int) queueEntryRepository.countByStatus(QUEUED);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean contains(Long executionId) {
        return queueEntryRepository.existsByExecutionIdAndStatus(executionId, QUEUED);
    }

    @Override
    public void onMessage(ConsumerRecord<String, String> record) {
        try {
            Long executionId = Long.parseLong(record.value());
            localBuffer.offer(executionId);
            log.debug("Received execution {} from Kafka topic {}", executionId, record.topic());
        } catch (NumberFormatException e) {
            log.warn("Ignoring invalid Kafka message: {}", record.value());
        }
    }

    @Transactional
    void markClaimed(Long executionId) {
        queueEntryRepository.findByExecutionIdAndStatus(executionId, QUEUED)
                .ifPresent(entry -> {
                    entry.setStatus("CLAIMED");
                    queueEntryRepository.save(entry);
                });
    }
}
