package com.fluffy.batch.engine;

/**
 * Determines how a job is executed.
 *
 * <ul>
 *   <li>{@code LOCAL} — the default; the job runs in-process using virtual threads.</li>
 *   <li>{@code CLOUD_NATIVE} — the job is delegated to an external orchestrator
 *       (Kubernetes, Airflow, AWS Batch, etc.) via a webhook / callback.</li>
 * </ul>
 */
public enum ExecutionMode {

    /** Run the job in-process (default behaviour). */
    LOCAL,

    /** Delegate execution to a cloud-native scheduler. */
    CLOUD_NATIVE
}
