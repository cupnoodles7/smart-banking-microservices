package com.smartbank.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enables binding of {@link JwtProperties}. Route definitions live in
 * {@code application.yml} under {@code spring.cloud.gateway.routes} so they can be
 * tuned without recompiling.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class GatewayConfig {
}
