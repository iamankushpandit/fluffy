package com.fluffy.aggregator;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AggregatorWebConfigTest {

    @Test
    void normalizePathAddsLeadingSlash() {
        assertThat(AggregatorWebConfig.normalizePath("my-agg")).isEqualTo("/my-agg");
    }

    @Test
    void normalizePathStripsTrailingSlash() {
        assertThat(AggregatorWebConfig.normalizePath("/my-agg/")).isEqualTo("/my-agg");
    }

    @Test
    void normalizePathReturnsDefaultForBlank() {
        assertThat(AggregatorWebConfig.normalizePath("  ")).isEqualTo("/fluffy-aggregator");
        assertThat(AggregatorWebConfig.normalizePath(null)).isEqualTo("/fluffy-aggregator");
    }

    @Test
    void redirectFilterRedirectsFromRootPath() throws Exception {
        AggregatorWebConfig.AggregatorDashboardRedirectFilter filter =
                new AggregatorWebConfig.AggregatorDashboardRedirectFilter("/fluffy-aggregator");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/fluffy-aggregator");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getHeader("Location")).endsWith("/fluffy-aggregator/index.html");
    }

    @Test
    void redirectFilterRedirectsFromTrailingSlashPath() throws Exception {
        AggregatorWebConfig.AggregatorDashboardRedirectFilter filter =
                new AggregatorWebConfig.AggregatorDashboardRedirectFilter("/fluffy-aggregator");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/fluffy-aggregator/");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getHeader("Location")).endsWith("/fluffy-aggregator/index.html");
    }

    @Test
    void redirectFilterPassesThroughOtherPaths() throws Exception {
        AggregatorWebConfig.AggregatorDashboardRedirectFilter filter =
                new AggregatorWebConfig.AggregatorDashboardRedirectFilter("/fluffy-aggregator");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/aggregator/summary");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }
}
