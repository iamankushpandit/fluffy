package com.fluffy.batch.backend;

import com.fluffy.batch.model.QueueEntry;
import com.fluffy.batch.persistence.QueueEntryRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaQueueBackendTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private QueueEntryRepository queueEntryRepository;

    private KafkaQueueBackend queueBackend;

    @BeforeEach
    void setUp() {
        queueBackend = new KafkaQueueBackend(kafkaTemplate, "test-topic", queueEntryRepository);
    }

    @Test
    void shouldEnqueueToKafkaAndDatabase() {
        queueBackend.enqueue(42L);

        ArgumentCaptor<org.apache.kafka.clients.producer.ProducerRecord<String, String>> captor =
                ArgumentCaptor.forClass(org.apache.kafka.clients.producer.ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());
        assertThat(captor.getValue().topic()).isEqualTo("test-topic");
        assertThat(captor.getValue().value()).isEqualTo("42");
        verify(queueEntryRepository).save(any(QueueEntry.class));
    }

    @Test
    void shouldPollFromLocalBuffer() {
        // Simulate Kafka message arrival
        ConsumerRecord<String, String> record = new ConsumerRecord<>("test-topic", 0, 0, "1", "1");
        queueBackend.onMessage(record);

        when(queueEntryRepository.findByExecutionIdAndStatus(1L, "QUEUED"))
                .thenReturn(Optional.of(new QueueEntry(1L, "", 1)));

        Long result = queueBackend.poll();
        assertThat(result).isEqualTo(1L);
    }

    @Test
    void shouldReturnNullWhenBufferEmpty() {
        assertThat(queueBackend.poll()).isNull();
    }

    @Test
    void shouldRemoveFromQueue() {
        when(queueEntryRepository.deleteByExecutionIdAndStatus(1L, "QUEUED")).thenReturn(1);

        boolean removed = queueBackend.remove(1L);
        assertThat(removed).isTrue();
    }

    @Test
    void shouldReturnFalseWhenRemovingNonExistent() {
        when(queueEntryRepository.deleteByExecutionIdAndStatus(999L, "QUEUED")).thenReturn(0);

        boolean removed = queueBackend.remove(999L);
        assertThat(removed).isFalse();
    }

    @Test
    void shouldCheckContains() {
        when(queueEntryRepository.existsByExecutionIdAndStatus(5L, "QUEUED")).thenReturn(true);
        when(queueEntryRepository.existsByExecutionIdAndStatus(99L, "QUEUED")).thenReturn(false);

        assertThat(queueBackend.contains(5L)).isTrue();
        assertThat(queueBackend.contains(99L)).isFalse();
    }

    @Test
    void shouldReturnPosition() {
        QueueEntry entry = new QueueEntry(10L, "", 3);
        when(queueEntryRepository.findByExecutionIdAndStatus(10L, "QUEUED")).thenReturn(Optional.of(entry));

        assertThat(queueBackend.getPosition(10L)).isEqualTo(3);
    }

    @Test
    void shouldReturnNullPositionForUnknown() {
        when(queueEntryRepository.findByExecutionIdAndStatus(999L, "QUEUED")).thenReturn(Optional.empty());

        assertThat(queueBackend.getPosition(999L)).isNull();
    }

    @Test
    void shouldReturnSize() {
        when(queueEntryRepository.countByStatus("QUEUED")).thenReturn(5L);

        assertThat(queueBackend.size()).isEqualTo(5);
    }

    @Test
    void shouldHandleInvalidKafkaMessage() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("test-topic", 0, 0, "key", "not-a-number");
        // Should not throw
        queueBackend.onMessage(record);
        assertThat(queueBackend.poll()).isNull();
    }
}
