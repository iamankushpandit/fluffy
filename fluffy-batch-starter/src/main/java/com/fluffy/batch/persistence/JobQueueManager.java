package com.fluffy.batch.persistence;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class JobQueueManager {

    private final Queue<Long> queue = new ConcurrentLinkedQueue<>();
    private final Map<Long, Integer> positionMap = new ConcurrentHashMap<>();
    private final AtomicInteger positionCounter = new AtomicInteger(0);

    public void enqueue(Long executionId) {
        int position = positionCounter.incrementAndGet();
        queue.offer(executionId);
        positionMap.put(executionId, position);
    }

    public Long poll() {
        Long executionId = queue.poll();
        if (executionId != null) {
            positionMap.remove(executionId);
        }
        return executionId;
    }

    public boolean remove(Long executionId) {
        boolean removed = queue.remove(executionId);
        if (removed) {
            positionMap.remove(executionId);
        }
        return removed;
    }

    public Integer getPosition(Long executionId) {
        return positionMap.get(executionId);
    }

    public int size() {
        return queue.size();
    }

    public boolean contains(Long executionId) {
        return positionMap.containsKey(executionId);
    }
}
