package com.fluffy.batch.engine;

import com.fluffy.batch.backend.CoordinationBackend;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory concurrency coordination for single-node / H2 mode.
 * State is not shared across nodes and will be lost on restart.
 */
public class ConcurrencyManager implements CoordinationBackend {

    private final Map<String, AtomicInteger> jobCounters = new ConcurrentHashMap<>();
    private final AtomicInteger globalCounter = new AtomicInteger(0);

    public boolean canRun(String jobName, int maxConcurrency) {
        AtomicInteger counter = jobCounters.computeIfAbsent(jobName, k -> new AtomicInteger(0));
        return counter.get() < maxConcurrency;
    }

    public void increment(String jobName) {
        jobCounters.computeIfAbsent(jobName, k -> new AtomicInteger(0)).incrementAndGet();
        globalCounter.incrementAndGet();
    }

    public void decrement(String jobName) {
        AtomicInteger counter = jobCounters.get(jobName);
        if (counter != null) {
            counter.decrementAndGet();
            globalCounter.decrementAndGet();
        }
    }

    public int getRunningCount(String jobName) {
        AtomicInteger counter = jobCounters.get(jobName);
        return counter != null ? counter.get() : 0;
    }

    public int getGlobalRunningCount() {
        return globalCounter.get();
    }
}
