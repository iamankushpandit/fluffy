package com.fluffy.batch.aggregator;

/**
 * Redirect controller for the aggregator dashboard.
 * <p>
 * This class is NOT annotated with {@code @Controller} because it must not
 * be picked up by component scanning.  The redirect is handled by the
 * filter registered in {@link AggregatorAutoConfiguration}.
 */
class AggregatorDashboardRedirect {
    // Redirect is handled by aggregatorDashboardRedirectFilter in AggregatorAutoConfiguration
}
