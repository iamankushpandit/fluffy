package com.fluffy.batch.engine;

import java.net.InetAddress;

/**
 * Shared utility for resolving the unique identifier of the current application node.
 * <p>
 * Used by {@link JobLauncher}, recovery, and database-backed queue components to
 * consistently identify which node owns a job execution or claimed a queue entry.
 * The node id is resolved once and cached for the lifetime of the JVM.
 * </p>
 * <p>
 * Resolution strategy:
 * <ol>
 *   <li>Use the local hostname ({@link InetAddress#getLocalHost()})</li>
 *   <li>Fall back to {@code node-<PID>} if the hostname cannot be determined</li>
 * </ol>
 * </p>
 */
public final class NodeIdResolver {

    private static final String NODE_ID = resolve();

    private NodeIdResolver() {
        // utility class
    }

    /**
     * Returns the cached node identifier for this JVM.
     */
    public static String getNodeId() {
        return NODE_ID;
    }

    private static String resolve() {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            return hostname != null ? hostname : "node-" + ProcessHandle.current().pid();
        } catch (Exception e) {
            return "node-" + ProcessHandle.current().pid();
        }
    }
}
