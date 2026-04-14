package com.fluffy.aggregator;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.client.RestClient;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;

/**
 * Web MVC configuration for the aggregator service.
 *
 * <p>Registers:
 * <ul>
 *   <li>A redirect filter: {@code /fluffy-aggregator} → {@code /fluffy-aggregator/index.html}</li>
 *   <li>A resource handler serving the React/MUI dashboard from the classpath.</li>
 *   <li>The {@link RestClient} bean used by {@link AggregatorService}.</li>
 * </ul>
 */
@Configuration
public class AggregatorWebConfig implements WebMvcConfigurer {

    private final AggregatorProperties properties;

    public AggregatorWebConfig(AggregatorProperties properties) {
        this.properties = properties;
    }

    @Bean
    public RestClient aggregatorRestClient() {
        return RestClient.create();
    }

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> aggregatorDashboardRedirectFilter() {
        String path = normalizePath(properties.getDashboardPath());
        FilterRegistrationBean<OncePerRequestFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new AggregatorDashboardRedirectFilter(path));
        reg.addUrlPatterns(path, path + "/");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return reg;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String path = normalizePath(properties.getDashboardPath());
        registry.addResourceHandler(path + "/**")
                .addResourceLocations("classpath:/static/fluffy-aggregator/");
    }

    static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/fluffy-aggregator";
        }
        String normalized = path.startsWith("/") ? path : "/" + path;
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /**
     * Redirect filter: bare {@code /fluffy-aggregator} or {@code /fluffy-aggregator/}
     * redirects to {@code /fluffy-aggregator/index.html}.
     */
    static class AggregatorDashboardRedirectFilter extends OncePerRequestFilter {

        private final String path;

        AggregatorDashboardRedirectFilter(String path) {
            this.path = path;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain)
                throws ServletException, IOException {
            String uri = request.getRequestURI();
            if (uri.equals(path) || uri.equals(path + "/")) {
                response.sendRedirect(path + "/index.html");
                return;
            }
            filterChain.doFilter(request, response);
        }
    }
}
