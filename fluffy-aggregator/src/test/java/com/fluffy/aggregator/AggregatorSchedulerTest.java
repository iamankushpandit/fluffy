package com.fluffy.aggregator;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class AggregatorSchedulerTest {

    @Test
    void pollDelegatesToAggregatorService() {
        AggregatorService service = mock(AggregatorService.class);
        NodeDiscoveryService discovery = mock(NodeDiscoveryService.class);
        AggregatorScheduler scheduler = new AggregatorScheduler(service, discovery);

        scheduler.poll();

        verify(service).pollNodes();
        verifyNoInteractions(discovery);
    }

    @Test
    void discoverDelegatesToNodeDiscoveryService() {
        AggregatorService service = mock(AggregatorService.class);
        NodeDiscoveryService discovery = mock(NodeDiscoveryService.class);
        AggregatorScheduler scheduler = new AggregatorScheduler(service, discovery);

        scheduler.discover();

        verify(discovery).refresh();
        verifyNoInteractions(service);
    }
}
