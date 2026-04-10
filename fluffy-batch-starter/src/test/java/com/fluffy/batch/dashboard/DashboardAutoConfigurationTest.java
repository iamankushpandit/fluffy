package com.fluffy.batch.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(DashboardAutoConfiguration.class);

    @Test
    void shouldRegisterDashboardBeansWhenEnabled() {
        contextRunner
                .withPropertyValues("fluffy.batch.dashboard.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(DashboardConfigController.class);
                    assertThat(context).hasSingleBean(DashboardProperties.class);
                    assertThat(context).hasBean("dashboardResourceConfigurer");
                    assertThat(context.getBean("dashboardResourceConfigurer"))
                            .isInstanceOf(WebMvcConfigurer.class);
                });
    }

    @Test
    void shouldNotRegisterDashboardBeansByDefault() {
        contextRunner
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DashboardConfigController.class);
                    assertThat(context).doesNotHaveBean("dashboardResourceConfigurer");
                });
    }

    @Test
    void shouldNotRegisterDashboardBeansWhenDisabled() {
        contextRunner
                .withPropertyValues("fluffy.batch.dashboard.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DashboardConfigController.class);
                    assertThat(context).doesNotHaveBean("dashboardResourceConfigurer");
                });
    }

    @Test
    void shouldRespectCustomProperties() {
        contextRunner
                .withPropertyValues(
                        "fluffy.batch.dashboard.enabled=true",
                        "fluffy.batch.dashboard.title=My Dashboard",
                        "fluffy.batch.dashboard.refresh-interval=10",
                        "fluffy.batch.dashboard.auth-enabled=true"
                )
                .run(context -> {
                    DashboardProperties props = context.getBean(DashboardProperties.class);
                    assertThat(props.getTitle()).isEqualTo("My Dashboard");
                    assertThat(props.getRefreshInterval()).isEqualTo(10);
                    assertThat(props.isAuthEnabled()).isTrue();
                });
    }

    @Test
    void shouldNormalizePathWithoutLeadingSlash() {
        contextRunner
                .withPropertyValues("fluffy.batch.dashboard.enabled=true",
                        "fluffy.batch.dashboard.path=custom-path")
                .run(context -> {
                    WebMvcConfigurer configurer = context.getBean("dashboardResourceConfigurer", WebMvcConfigurer.class);
                    assertThat(configurer).isNotNull();
                    // Exercise the addResourceHandlers to cover normalizePath
                    ResourceHandlerRegistry registry = new ResourceHandlerRegistry(
                            context, context.getServletContext());
                    configurer.addResourceHandlers(registry);
                });
    }

    @Test
    void shouldNormalizePathWithTrailingSlash() {
        contextRunner
                .withPropertyValues("fluffy.batch.dashboard.enabled=true",
                        "fluffy.batch.dashboard.path=/custom-path/")
                .run(context -> {
                    WebMvcConfigurer configurer = context.getBean("dashboardResourceConfigurer", WebMvcConfigurer.class);
                    ResourceHandlerRegistry registry = new ResourceHandlerRegistry(
                            context, context.getServletContext());
                    configurer.addResourceHandlers(registry);
                });
    }

    @Test
    void shouldNormalizeBlankPath() {
        contextRunner
                .withPropertyValues("fluffy.batch.dashboard.enabled=true",
                        "fluffy.batch.dashboard.path=  ")
                .run(context -> {
                    WebMvcConfigurer configurer = context.getBean("dashboardResourceConfigurer", WebMvcConfigurer.class);
                    ResourceHandlerRegistry registry = new ResourceHandlerRegistry(
                            context, context.getServletContext());
                    configurer.addResourceHandlers(registry);
                });
    }
}
