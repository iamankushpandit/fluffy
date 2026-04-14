package com.fluffy.batch.autoconfigure;

import com.fluffy.batch.engine.CloudNativeExecutionStrategy;
import com.fluffy.batch.engine.ExecutionCallback;
import com.fluffy.batch.engine.LocalExecutionStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CloudNativeAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CloudNativeAutoConfiguration.class))
            .withUserConfiguration(TestDependencies.class);

    @Test
    void shouldRegisterLocalExecutionStrategyByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(LocalExecutionStrategy.class);
            assertThat(context).doesNotHaveBean(CloudNativeExecutionStrategy.class);
        });
    }

    @Test
    void shouldRegisterCloudNativeStrategyWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "fluffy.batch.cloud-native.enabled=true",
                        "fluffy.batch.cloud-native.endpoint=http://example.com/api"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(LocalExecutionStrategy.class);
                    assertThat(context).hasSingleBean(CloudNativeExecutionStrategy.class);
                });
    }

    @Test
    void shouldNotRegisterCloudNativeStrategyWhenDisabled() {
        contextRunner
                .withPropertyValues("fluffy.batch.cloud-native.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(LocalExecutionStrategy.class);
                    assertThat(context).doesNotHaveBean(CloudNativeExecutionStrategy.class);
                });
    }

    @Test
    void shouldNotOverrideExistingLocalExecutionStrategyBean() {
        contextRunner
                .withUserConfiguration(CustomLocalStrategy.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(LocalExecutionStrategy.class);
                    // The custom one should be the one registered
                    assertThat(context.getBean(LocalExecutionStrategy.class)).isNotNull();
                });
    }

    @Test
    void shouldBindCloudNativeProperties() {
        contextRunner
                .withPropertyValues(
                        "fluffy.batch.cloud-native.enabled=true",
                        "fluffy.batch.cloud-native.endpoint=http://test.com",
                        "fluffy.batch.cloud-native.callback-url=http://cb.com",
                        "fluffy.batch.cloud-native.connect-timeout-seconds=5",
                        "fluffy.batch.cloud-native.request-timeout-seconds=15"
                )
                .run(context -> {
                    CloudNativeProperties props = context.getBean(CloudNativeProperties.class);
                    assertThat(props.isEnabled()).isTrue();
                    assertThat(props.getEndpoint()).isEqualTo("http://test.com");
                    assertThat(props.getCallbackUrl()).isEqualTo("http://cb.com");
                    assertThat(props.getConnectTimeoutSeconds()).isEqualTo(5);
                    assertThat(props.getRequestTimeoutSeconds()).isEqualTo(15);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class TestDependencies {

        @Bean("jobExecutorService")
        ExecutorService jobExecutorService() {
            return Executors.newSingleThreadExecutor();
        }

        @Bean("jobScheduledExecutorService")
        ScheduledExecutorService jobScheduledExecutorService() {
            return Executors.newSingleThreadScheduledExecutor();
        }

        @Bean
        ExecutionCallback executionCallback() {
            return mock(ExecutionCallback.class);
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomLocalStrategy {

        @Bean
        LocalExecutionStrategy localExecutionStrategy() {
            return new LocalExecutionStrategy(
                    Executors.newSingleThreadExecutor(),
                    Executors.newSingleThreadScheduledExecutor(),
                    mock(ExecutionCallback.class)
            );
        }
    }
}
