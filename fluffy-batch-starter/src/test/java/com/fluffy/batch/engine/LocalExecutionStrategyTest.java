package com.fluffy.batch.engine;

import com.fluffy.batch.api.JobContext;
import com.fluffy.batch.api.JobHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalExecutionStrategyTest {

    @Mock
    private ExecutorService executorService;

    @Mock
    private ScheduledExecutorService scheduledExecutorService;

    @Mock
    private ExecutionCallback callback;

    private LocalExecutionStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new LocalExecutionStrategy(executorService, scheduledExecutorService, callback);
    }

    @Test
    void getMode_shouldReturnLocal() {
        assertThat(strategy.getMode()).isEqualTo(ExecutionMode.LOCAL);
    }

    @Test
    void executeSyncJob_shouldCallCallbacksInOrder() {
        JobHandler handler = ctx -> {};
        JobDefinition def = JobDefinition.builder("syncJob")
                .handler(handler)
                .async(false)
                .maxConcurrency(1)
                .build();
        JobContext context = new JobContext(1L, "syncJob", "user", Collections.emptyMap(), null);

        strategy.execute(1L, def, context);

        var inOrder = inOrder(callback);
        inOrder.verify(callback).onStarted(1L);
        inOrder.verify(callback).onCompleted(1L, "syncJob");
        inOrder.verify(callback).onFinished(1L, "syncJob", 1);
    }

    @Test
    void executeSyncJob_shouldReportFailedOnException() {
        JobHandler handler = ctx -> { throw new RuntimeException("boom"); };
        JobDefinition def = JobDefinition.builder("failJob")
                .handler(handler)
                .async(false)
                .maxConcurrency(2)
                .build();
        JobContext context = new JobContext(2L, "failJob", "user", Collections.emptyMap(), null);

        strategy.execute(2L, def, context);

        verify(callback).onStarted(2L);
        verify(callback).onFailed(2L, "failJob", "boom");
        verify(callback).onFinished(2L, "failJob", 2);
        verify(callback, never()).onCompleted(anyLong(), anyString());
    }

    @Test
    void executeSyncJob_shouldReportStoppedOnInterruptedException() {
        JobHandler handler = ctx -> { throw new InterruptedException("stop"); };
        JobDefinition def = JobDefinition.builder("interruptedJob")
                .handler(handler)
                .async(false)
                .maxConcurrency(1)
                .build();
        JobContext context = new JobContext(3L, "interruptedJob", "user", Collections.emptyMap(), null);

        strategy.execute(3L, def, context);

        verify(callback).onStarted(3L);
        verify(callback).onStopped(3L, "interruptedJob");
        verify(callback).onFinished(3L, "interruptedJob", 1);
        verify(callback, never()).onCompleted(anyLong(), anyString());
        verify(callback, never()).onFailed(anyLong(), anyString(), anyString());
    }

    @Test
    void executeSyncJob_shouldRemoveFromRunningContextsAfterCompletion() {
        JobHandler handler = ctx -> {};
        JobDefinition def = JobDefinition.builder("job")
                .handler(handler)
                .async(false)
                .build();
        JobContext context = new JobContext(4L, "job", "user", Collections.emptyMap(), null);

        strategy.execute(4L, def, context);

        assertThat(strategy.getRunningContexts()).doesNotContainKey(4L);
    }

    @Test
    void executeAsyncJob_shouldSubmitToExecutorService() {
        JobHandler handler = ctx -> {};
        JobDefinition def = JobDefinition.builder("asyncJob")
                .handler(handler)
                .async(true)
                .timeoutSeconds(0)
                .maxConcurrency(1)
                .build();
        JobContext context = new JobContext(5L, "asyncJob", "user", Collections.emptyMap(), null);

        Future<?> mockFuture = mock(Future.class);
        doReturn(mockFuture).when(executorService).submit(any(Runnable.class));

        strategy.execute(5L, def, context);

        verify(executorService).submit(any(Runnable.class));
        assertThat(strategy.getRunningContexts()).containsKey(5L);
    }

    @Test
    void executeAsyncJob_shouldScheduleTimeoutWhenTimeoutPositive() {
        JobHandler handler = ctx -> {};
        JobDefinition def = JobDefinition.builder("timeoutJob")
                .handler(handler)
                .async(true)
                .timeoutSeconds(60)
                .maxConcurrency(1)
                .build();
        JobContext context = new JobContext(6L, "timeoutJob", "user", Collections.emptyMap(), null);

        Future<?> mockFuture = mock(Future.class);
        doReturn(mockFuture).when(executorService).submit(any(Runnable.class));

        strategy.execute(6L, def, context);

        verify(scheduledExecutorService).schedule(any(Runnable.class), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    void executeAsyncJob_shouldNotScheduleTimeoutWhenZero() {
        JobHandler handler = ctx -> {};
        JobDefinition def = JobDefinition.builder("noTimeoutJob")
                .handler(handler)
                .async(true)
                .timeoutSeconds(0)
                .maxConcurrency(1)
                .build();
        JobContext context = new JobContext(7L, "noTimeoutJob", "user", Collections.emptyMap(), null);

        Future<?> mockFuture = mock(Future.class);
        doReturn(mockFuture).when(executorService).submit(any(Runnable.class));

        strategy.execute(7L, def, context);

        verify(scheduledExecutorService, never()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    void executeAsyncJob_capturedRunnableCallsCallbacks() {
        JobHandler handler = ctx -> {};
        JobDefinition def = JobDefinition.builder("asyncJob2")
                .handler(handler)
                .async(true)
                .timeoutSeconds(0)
                .maxConcurrency(3)
                .build();
        JobContext context = new JobContext(8L, "asyncJob2", "user", Collections.emptyMap(), null);

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        Future<?> mockFuture = mock(Future.class);
        doReturn(mockFuture).when(executorService).submit(runnableCaptor.capture());

        strategy.execute(8L, def, context);

        // Run the captured Runnable to simulate async execution
        runnableCaptor.getValue().run();

        verify(callback).onStarted(8L);
        verify(callback).onCompleted(8L, "asyncJob2");
        verify(callback).onFinished(8L, "asyncJob2", 3);
    }

    @Test
    void executeAsyncJob_capturedRunnableReportsFailure() {
        JobHandler handler = ctx -> { throw new RuntimeException("async boom"); };
        JobDefinition def = JobDefinition.builder("asyncFailJob")
                .handler(handler)
                .async(true)
                .timeoutSeconds(0)
                .maxConcurrency(1)
                .build();
        JobContext context = new JobContext(9L, "asyncFailJob", "user", Collections.emptyMap(), null);

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        Future<?> mockFuture = mock(Future.class);
        doReturn(mockFuture).when(executorService).submit(runnableCaptor.capture());

        strategy.execute(9L, def, context);
        runnableCaptor.getValue().run();

        verify(callback).onStarted(9L);
        verify(callback).onFailed(9L, "asyncFailJob", "async boom");
        verify(callback).onFinished(9L, "asyncFailJob", 1);
    }

    @Test
    void stop_shouldReturnTrueForRunningJob() {
        JobHandler handler = ctx -> {};
        JobDefinition def = JobDefinition.builder("runningJob")
                .handler(handler)
                .async(true)
                .timeoutSeconds(0)
                .build();
        JobContext context = new JobContext(10L, "runningJob", "user", Collections.emptyMap(), null);

        Future<?> mockFuture = mock(Future.class);
        doReturn(mockFuture).when(executorService).submit(any(Runnable.class));

        strategy.execute(10L, def, context);

        boolean result = strategy.stop(10L);

        assertThat(result).isTrue();
        assertThat(context.isStopRequested()).isTrue();
        verify(mockFuture).cancel(true);
    }

    @Test
    void stop_shouldReturnFalseForNonRunningJob() {
        boolean result = strategy.stop(999L);
        assertThat(result).isFalse();
    }

    @Test
    void stop_shouldHandleMissingFutureGracefully() {
        // Manually add context without future (simulates sync execution in progress)
        JobContext context = new JobContext(11L, "job", "user", Collections.emptyMap(), null);
        strategy.getRunningContexts().put(11L, context);

        boolean result = strategy.stop(11L);

        assertThat(result).isTrue();
        assertThat(context.isStopRequested()).isTrue();
    }

    @Test
    void executeAsyncJob_timeoutRunnable_shouldCancelFuture() {
        JobHandler handler = ctx -> {};
        JobDefinition def = JobDefinition.builder("timeoutExecJob")
                .handler(handler)
                .async(true)
                .timeoutSeconds(30)
                .maxConcurrency(1)
                .build();
        JobContext context = new JobContext(12L, "timeoutExecJob", "user", Collections.emptyMap(), null);

        Future<?> mockFuture = mock(Future.class);
        doReturn(mockFuture).when(executorService).submit(any(Runnable.class));

        ArgumentCaptor<Runnable> timeoutCaptor = ArgumentCaptor.forClass(Runnable.class);
        doReturn(null).when(scheduledExecutorService).schedule(timeoutCaptor.capture(), eq(30L), eq(TimeUnit.SECONDS));

        strategy.execute(12L, def, context);

        // Run the timeout runnable — context is still in runningContexts
        timeoutCaptor.getValue().run();

        assertThat(context.isStopRequested()).isTrue();
        verify(mockFuture).cancel(true);
    }

    @Test
    void executeAsyncJob_timeoutRunnable_shouldNotCancelIfAlreadyFinished() {
        JobHandler handler = ctx -> {};
        JobDefinition def = JobDefinition.builder("finishedJob")
                .handler(handler)
                .async(true)
                .timeoutSeconds(10)
                .maxConcurrency(1)
                .build();
        JobContext context = new JobContext(13L, "finishedJob", "user", Collections.emptyMap(), null);

        Future<?> mockFuture = mock(Future.class);
        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        doReturn(mockFuture).when(executorService).submit(taskCaptor.capture());

        ArgumentCaptor<Runnable> timeoutCaptor = ArgumentCaptor.forClass(Runnable.class);
        doReturn(null).when(scheduledExecutorService).schedule(timeoutCaptor.capture(), eq(10L), eq(TimeUnit.SECONDS));

        strategy.execute(13L, def, context);

        // Run the job first (cleans up runningContexts)
        taskCaptor.getValue().run();

        // Now run timeout — should be a no-op since context already removed
        timeoutCaptor.getValue().run();

        assertThat(context.isStopRequested()).isFalse();
    }

    @Test
    void getRunningContexts_shouldReturnMutableMap() {
        assertThat(strategy.getRunningContexts()).isNotNull();
        assertThat(strategy.getRunningContexts()).isEmpty();
    }
}
