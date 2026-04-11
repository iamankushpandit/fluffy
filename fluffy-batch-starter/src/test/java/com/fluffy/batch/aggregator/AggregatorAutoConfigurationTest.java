package com.fluffy.batch.aggregator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.assertj.core.api.Assertions.assertThat;

class AggregatorAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(AggregatorAutoConfiguration.class);

    @Test
    void shouldRegisterBeansWhenEnabled() {
        contextRunner
                .withPropertyValues("fluffy.batch.aggregator.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(AggregatorProperties.class);
                    assertThat(context).hasSingleBean(NodeDiscoveryService.class);
                    assertThat(context).hasSingleBean(AggregatorService.class);
                    assertThat(context).hasSingleBean(AggregatorController.class);
                    assertThat(context).hasSingleBean(AggregatorAutoConfiguration.AggregatorScheduler.class);
                    assertThat(context).hasBean("aggregatorDashboardConfigurer");
                    assertThat(context.getBean("aggregatorDashboardConfigurer"))
                            .isInstanceOf(WebMvcConfigurer.class);
                });
    }

    @Test
    void shouldNotRegisterBeansByDefault() {
        contextRunner
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AggregatorController.class);
                    assertThat(context).doesNotHaveBean(NodeDiscoveryService.class);
                    assertThat(context).doesNotHaveBean(AggregatorService.class);
                });
    }

    @Test
    void shouldNotRegisterBeansWhenDisabled() {
        contextRunner
                .withPropertyValues("fluffy.batch.aggregator.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AggregatorController.class);
                    assertThat(context).doesNotHaveBean(NodeDiscoveryService.class);
                });
    }

    @Test
    void shouldRespectCustomProperties() {
        contextRunner
                .withPropertyValues(
                        "fluffy.batch.aggregator.enabled=true",
                        "fluffy.batch.aggregator.poll-interval-seconds=5",
                        "fluffy.batch.aggregator.discovery-interval-seconds=60",
                        "fluffy.batch.aggregator.dashboard-path=/custom-agg"
                )
                .run(context -> {
                    AggregatorProperties props = context.getBean(AggregatorProperties.class);
                    assertThat(props.getPollIntervalSeconds()).isEqualTo(5);
                    assertThat(props.getDiscoveryIntervalSeconds()).isEqualTo(60);
                    assertThat(props.getDashboardPath()).isEqualTo("/custom-agg");
                });
    }

    @Test
    void shouldNormalizeCustomDashboardPath() {
        contextRunner
                .withPropertyValues(
                        "fluffy.batch.aggregator.enabled=true",
                        "fluffy.batch.aggregator.dashboard-path=custom-agg/"
                )
                .run(context -> {
                    WebMvcConfigurer configurer = context.getBean(
                            "aggregatorDashboardConfigurer", WebMvcConfigurer.class);
                    assertThat(configurer).isNotNull();
                    ResourceHandlerRegistry registry = new ResourceHandlerRegistry(
                            context, context.getServletContext());
                    configurer.addResourceHandlers(registry);
                });
    }

    @Test
    void shouldNormalizeBlankDashboardPath() {
        contextRunner
                .withPropertyValues(
                        "fluffy.batch.aggregator.enabled=true",
                        "fluffy.batch.aggregator.dashboard-path=  "
                )
                .run(context -> {
                    WebMvcConfigurer configurer = context.getBean(
                            "aggregatorDashboardConfigurer", WebMvcConfigurer.class);
                    ResourceHandlerRegistry registry = new ResourceHandlerRegistry(
                            context, context.getServletContext());
                    configurer.addResourceHandlers(registry);
                });
    }
}
