package com.fluffy.batch.dashboard;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnProperty(name = "fluffy.batch.dashboard.enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(DashboardProperties.class)
public class DashboardAutoConfiguration {

    @Bean
    public DashboardConfigController dashboardConfigController(DashboardProperties properties) {
        return new DashboardConfigController(properties);
    }

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> dashboardRedirectFilter(
            DashboardProperties properties) {
        String path = normalizePath(properties.getPath());
        FilterRegistrationBean<OncePerRequestFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                    jakarta.servlet.http.HttpServletResponse response,
                    jakarta.servlet.FilterChain filterChain)
                    throws jakarta.servlet.ServletException, java.io.IOException {
                String uri = request.getRequestURI();
                if (uri.equals(path) || uri.equals(path + "/")) {
                    response.sendRedirect(path + "/index.html");
                    return;
                }
                filterChain.doFilter(request, response);
            }
        });
        reg.addUrlPatterns(path, path + "/");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return reg;
    }

    @Bean
    public WebMvcConfigurer dashboardResourceConfigurer(DashboardProperties properties) {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                String path = normalizePath(properties.getPath());
                registry.addResourceHandler(path + "/**")
                        .addResourceLocations("classpath:/static/fluffy-dashboard/");
            }
        };
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/fluffy-dashboard";
        }
        String normalized = path.startsWith("/") ? path : "/" + path;
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
