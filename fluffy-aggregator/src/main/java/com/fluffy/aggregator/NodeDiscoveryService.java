package com.fluffy.aggregator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Discovers and maintains the live list of Fluffy node base URLs.
 *
 * <p>In its simplest form the list comes from
 * {@link AggregatorProperties#getNodes()}. The service refreshes periodically
 * so that nodes added or removed at runtime (e.g. via Kubernetes scaling) are
 * picked up.
 */
@Service
public class NodeDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(NodeDiscoveryService.class);

    private final AggregatorProperties properties;
    private final List<String> discoveredNodes = new CopyOnWriteArrayList<>();

    public NodeDiscoveryService(AggregatorProperties properties) {
        this.properties = properties;
        refresh();
    }

    /** Re-reads the configured node list. */
    public void refresh() {
        List<String> configured = properties.getNodes();
        if (configured == null || configured.isEmpty()) {
            log.debug("No aggregator nodes configured");
            discoveredNodes.clear();
            return;
        }
        List<String> normalized = new ArrayList<>();
        for (String url : configured) {
            if (url != null && !url.isBlank()) {
                String trimmed = url.strip();
                while (trimmed.endsWith("/")) {
                    trimmed = trimmed.substring(0, trimmed.length() - 1);
                }
                normalized.add(trimmed);
            }
        }
        discoveredNodes.clear();
        discoveredNodes.addAll(normalized);
        log.info("Discovered {} aggregator node(s): {}", discoveredNodes.size(), discoveredNodes);
    }

    /** Returns an unmodifiable snapshot of currently known node URLs. */
    public List<String> getNodes() {
        return Collections.unmodifiableList(new ArrayList<>(discoveredNodes));
    }
}
