package com.example.microservice.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Configuration
@Slf4j
public class MetricsConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            try {
                String hostname = InetAddress.getLocalHost().getHostName();
                registry.config().commonTags(
                        "application", "microservice-template",
                        "hostname", hostname,
                        "environment", System.getProperty("spring.profiles.active", "dev")
                );
            } catch (UnknownHostException e) {
                log.warn("Failed to get hostname for metrics", e);
            }
        };
    }

    @Bean
    public MeterBinder customMetrics() {
        return registry -> {
            // Custom business metrics can be registered here
            // Example:
            // Counter.builder("custom.requests.total")
            //     .description("Total number of custom requests")
            //     .register(registry);
        };
    }
}