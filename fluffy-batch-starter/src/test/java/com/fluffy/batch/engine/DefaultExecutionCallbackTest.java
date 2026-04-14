package com.fluffy.batch.engine;

import com.fluffy.batch.backend.CoordinationBackend;
import com.fluffy.batch.model.JobExecution;
import com.fluffy.batch.persistence.JobExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultExecutionCallbackTest {

    @Mock
    private JobExecutionRepository executionRepository;

    @Mock
    private CoordinationBackend coordinationBackend;

    private DefaultExecutionCallback callback;

    @BeforeEach
    void setUp() {
        callback = new DefaultExecutionCallback(executionRepository, coordinationBackend);
    }

    @Test
    void onStarted_shouldSetStatusToStartedAndClearQueuePosition() {
        JobExecution exec = new JobExecution();
        exec.setId(1L);
        exec.setQueuePosition(5);
        when(executionRepository.findById(1L)).thenReturn(Optional.of(exec));

        callback.onStarted(1L);

        assertThat(exec.getStatus()).isEqualTo(BatchStatus.STARTED);
        assertThat(exec.getQueuePosition()).isNull();
        verify(executionRepository).save(exec);
    }

    @Test
    void onStarted_shouldDoNothingWhenExecutionNotFound() {
        when(executionRepository.findById(99L)).thenReturn(Optional.empty());

        callback.onStarted(99L);

        verify(executionRepository, never()).save(any());
    }

    @Test
    void onCompleted_shouldSetStatusToCompleted() {
        JobExecution exec = new JobExecution();
        exec.setId(2L);
        when(executionRepository.findById(2L)).thenReturn(Optional.of(exec));

        callback.onCompleted(2L, "testJob");

        assertThat(exec.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        verify(executionRepository).save(exec);
    }

    @Test
    void onCompleted_shouldDoNothingWhenExecutionNotFound() {
        when(executionRepository.findById(99L)).thenReturn(Optional.empty());

        callback.onCompleted(99L, "testJob");

        verify(executionRepository, never()).save(any());
    }

    @Test
    void onStopped_shouldSetStatusToStopped() {
        JobExecution exec = new JobExecution();
        exec.setId(3L);
        when(executionRepository.findById(3L)).thenReturn(Optional.of(exec));

        callback.onStopped(3L, "testJob");

        assertThat(exec.getStatus()).isEqualTo(BatchStatus.STOPPED);
        verify(executionRepository).save(exec);
    }

    @Test
    void onStopped_shouldDoNothingWhenExecutionNotFound() {
        when(executionRepository.findById(99L)).thenReturn(Optional.empty());

        callback.onStopped(99L, "testJob");

        verify(executionRepository, never()).save(any());
    }

    @Test
    void onFailed_shouldSetStatusToFailedWithErrorMessage() {
        JobExecution exec = new JobExecution();
        exec.setId(4L);
        when(executionRepository.findById(4L)).thenReturn(Optional.of(exec));

        callback.onFailed(4L, "testJob", "Something went wrong");

        assertThat(exec.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(exec.getErrorMessage()).isEqualTo("Something went wrong");
        verify(executionRepository).save(exec);
    }

    @Test
    void onFailed_shouldTruncateErrorMessageAt4096Chars() {
        JobExecution exec = new JobExecution();
        exec.setId(5L);
        when(executionRepository.findById(5L)).thenReturn(Optional.of(exec));

        String longMessage = "x".repeat(5000);
        callback.onFailed(5L, "testJob", longMessage);

        assertThat(exec.getErrorMessage()).hasSize(4096);
        assertThat(exec.getErrorMessage()).isEqualTo("x".repeat(4096));
        verify(executionRepository).save(exec);
    }

    @Test
    void onFailed_shouldHandleNullErrorMessage() {
        JobExecution exec = new JobExecution();
        exec.setId(6L);
        when(executionRepository.findById(6L)).thenReturn(Optional.of(exec));

        callback.onFailed(6L, "testJob", null);

        assertThat(exec.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(exec.getErrorMessage()).isNull();
        verify(executionRepository).save(exec);
    }

    @Test
    void onFailed_shouldNotTruncateShortErrorMessage() {
        JobExecution exec = new JobExecution();
        exec.setId(7L);
        when(executionRepository.findById(7L)).thenReturn(Optional.of(exec));

        callback.onFailed(7L, "testJob", "short error");

        assertThat(exec.getErrorMessage()).isEqualTo("short error");
    }

    @Test
    void onFailed_shouldDoNothingWhenExecutionNotFound() {
        when(executionRepository.findById(99L)).thenReturn(Optional.empty());

        callback.onFailed(99L, "testJob", "error");

        verify(executionRepository, never()).save(any());
    }

    @Test
    void onFinished_shouldDecrementCoordinationAndSetEndTime() {
        JobExecution exec = new JobExecution();
        exec.setId(8L);
        when(executionRepository.findById(8L)).thenReturn(Optional.of(exec));

        callback.onFinished(8L, "testJob", 3);

        verify(coordinationBackend).decrement("testJob");
        assertThat(exec.getEndTime()).isNotNull();
        verify(executionRepository).save(exec);
    }

    @Test
    void onFinished_shouldCallQueueProcessorWhenSet() {
        JobExecution exec = new JobExecution();
        exec.setId(9L);
        when(executionRepository.findById(9L)).thenReturn(Optional.of(exec));

        DefaultExecutionCallback.QueueProcessor processor = mock(DefaultExecutionCallback.QueueProcessor.class);
        callback.setQueueProcessor(processor);

        callback.onFinished(9L, "testJob", 5);

        verify(processor).processQueue("testJob", 5);
    }

    @Test
    void onFinished_shouldNotCallQueueProcessorWhenNotSet() {
        JobExecution exec = new JobExecution();
        exec.setId(10L);
        when(executionRepository.findById(10L)).thenReturn(Optional.of(exec));

        // queueProcessor not set — should not throw
        callback.onFinished(10L, "testJob", 3);

        verify(coordinationBackend).decrement("testJob");
    }

    @Test
    void onFinished_shouldDoNothingForEndTimeWhenExecutionNotFound() {
        when(executionRepository.findById(99L)).thenReturn(Optional.empty());

        callback.onFinished(99L, "testJob", 3);

        verify(coordinationBackend).decrement("testJob");
        verify(executionRepository, never()).save(any());
    }

    @Test
    void onFailed_shouldKeepExactly4096CharsWhenMessageIs4096() {
        JobExecution exec = new JobExecution();
        exec.setId(11L);
        when(executionRepository.findById(11L)).thenReturn(Optional.of(exec));

        String exactMessage = "a".repeat(4096);
        callback.onFailed(11L, "testJob", exactMessage);

        assertThat(exec.getErrorMessage()).hasSize(4096);
    }

    @Test
    void setQueueProcessor_shouldAllowOverride() {
        DefaultExecutionCallback.QueueProcessor p1 = mock(DefaultExecutionCallback.QueueProcessor.class);
        DefaultExecutionCallback.QueueProcessor p2 = mock(DefaultExecutionCallback.QueueProcessor.class);

        JobExecution exec = new JobExecution();
        exec.setId(12L);
        when(executionRepository.findById(12L)).thenReturn(Optional.of(exec));

        callback.setQueueProcessor(p1);
        callback.setQueueProcessor(p2);

        callback.onFinished(12L, "job", 1);

        verify(p1, never()).processQueue(anyString(), anyInt());
        verify(p2).processQueue("job", 1);
    }
}
