package com.fluffy.batch.autoconfigure;

import com.fluffy.batch.web.ScalingConfigController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for scaling configuration and REST API.
 */
@AutoConfiguration
@ConditionalOnWebApplication
@EnableConfigurationProperties(ScalingProperties.class)
@Import(ScalingConfigController.class)
public class ScalingAutoConfiguration {
}
