package com.template.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Custom route definitions for the API Gateway.
 *
 * <p>Routes are defined programmatically here as an alternative to YAML-based
 * configuration, providing better IDE support and compile-time safety.
 * Routes can also be configured in {@code application.yml} under
 * {@code spring.cloud.gateway.server.webflux.routes} (Spring Boot 4+).</p>
 */
@Configuration
@Slf4j
public class GatewayRouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Route: microservice-template service
                .route("microservice-template", r -> r
                        .path("/api/**")
                        .uri("lb://microservice-template"))
                // Route: actuator endpoints (passthrough to management)
                .route("microservice-template-actuator", r -> r
                        .path("/manage/**")
                        .filters(f -> f
                                .stripPrefix(0))
                        .uri("lb://microservice-template"))
                // Route: Swagger UI passthrough
                .route("microservice-template-swagger", r -> r
                        .path("/swagger-ui/**", "/v3/api-docs/**",
                                "/webjars/**", "/swagger-ui.html")
                        .uri("lb://microservice-template"))
                // Fallback: all other requests
                .route("fallback", r -> r
                        .path("/**")
                        .uri("lb://microservice-template"))
                .build();
    }
}
