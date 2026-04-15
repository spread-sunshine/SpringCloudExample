package com.template.microservice.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
            // Business metrics
            Counter.builder("microservice.requests.total")
                .description("Total number of requests")
                .tag("type", "http")
                .register(registry);
            
            Counter.builder("microservice.authentication.success")
                .description("Successful authentication attempts")
                .register(registry);
            
            Counter.builder("microservice.authentication.failure")
                .description("Failed authentication attempts")
                .register(registry);
            
            Counter.builder("microservice.authorization.failure")
                .description("Authorization failures")
                .register(registry);
            
            Counter.builder("microservice.api.key.valid")
                .description("Valid API key validations")
                .register(registry);
            
            Counter.builder("microservice.api.key.invalid")
                .description("Invalid API key validations")
                .register(registry);
            
            Counter.builder("microservice.jwt.blacklist.added")
                .description("Tokens added to JWT blacklist")
                .register(registry);
            
            Counter.builder("microservice.jwt.blacklist.checked")
                .description("JWT blacklist checks")
                .register(registry);
            
            Counter.builder("microservice.jwt.blacklist.hits")
                .description("Blacklisted JWT tokens found")
                .register(registry);
            
            Counter.builder("microservice.rate.limit.exceeded")
                .description("Rate limit exceeded events")
                .register(registry);
            
            Counter.builder("microservice.circuit.breaker.opened")
                .description("Circuit breaker opened events")
                .register(registry);
            
            Counter.builder("microservice.database.queries")
                .description("Database query count")
                .register(registry);
            
            Counter.builder("microservice.cache.hits")
                .description("Cache hit count")
                .register(registry);
            
            Counter.builder("microservice.cache.misses")
                .description("Cache miss count")
                .register(registry);
            
            Counter.builder("microservice.message.queue.sent")
                .description("Messages sent to queue")
                .tag("queue", "rabbitmq")
                .register(registry);
            
            Counter.builder("microservice.message.queue.received")
                .description("Messages received from queue")
                .tag("queue", "rabbitmq")
                .register(registry);
            
            Counter.builder("microservice.kafka.messages.sent")
                .description("Messages sent to Kafka")
                .register(registry);
            
            Counter.builder("microservice.kafka.messages.received")
                .description("Messages received from Kafka")
                .register(registry);
            
            // Gauge for active API keys
            registry.gauge("microservice.api.keys.active", 
                registry, 
                r -> (double) r.find("microservice.api.key.valid").counters().size());
            
            // Gauge for JWT blacklist size
            registry.gauge("microservice.jwt.blacklist.size",
                registry,
                r -> (double) r.find("microservice.jwt.blacklist.added").counters().stream()
                    .mapToDouble(c -> c.count()).sum());
            
            // Timer for API response times
            Timer.builder("microservice.response.time")
                .description("API response time distribution")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
            
            // Timer for database query times
            Timer.builder("microservice.database.query.time")
                .description("Database query time distribution")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
        };
    }
}