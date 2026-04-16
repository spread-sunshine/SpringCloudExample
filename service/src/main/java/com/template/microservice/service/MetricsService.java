package com.template.microservice.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsService {

    private final MeterRegistry meterRegistry;

    // Business metrics
    public void incrementRequestCount(String endpoint) {
        Counter.builder("business.requests.total")
                .tag("endpoint", endpoint)
                .description("Total number of business requests")
                .register(meterRegistry)
                .increment();
    }

    public void incrementSuccessfulRequest(String endpoint) {
        Counter.builder("business.requests.successful")
                .tag("endpoint", endpoint)
                .description("Number of successful business requests")
                .register(meterRegistry)
                .increment();
    }

    public void incrementFailedRequest(String endpoint, String errorCode) {
        Counter.builder("business.requests.failed")
                .tag("endpoint", endpoint)
                .tag("error_code", errorCode)
                .description("Number of failed business requests")
                .register(meterRegistry)
                .increment();
    }

    public void recordProcessingTime(String operation, long duration, TimeUnit unit) {
        Timer.builder("business.processing.time")
                .tag("operation", operation)
                .description("Time taken to process business operations")
                .register(meterRegistry)
                .record(duration, unit);
    }

    public void recordResponseSize(String endpoint, long size) {
        DistributionSummary.builder("business.response.size")
                .tag("endpoint", endpoint)
                .description("Size of business responses")
                .register(meterRegistry)
                .record(size);
    }

    // User metrics
    public void incrementUserRegistration() {
        Counter.builder("user.registrations.total")
                .description("Total number of user registrations")
                .register(meterRegistry)
                .increment();
    }

    public void incrementUserLogin() {
        Counter.builder("user.logins.total")
                .description("Total number of user logins")
                .register(meterRegistry)
                .increment();
    }

    public void incrementUserLogout() {
        Counter.builder("user.logouts.total")
                .description("Total number of user logouts")
                .register(meterRegistry)
                .increment();
    }

    // Order metrics
    public void incrementOrderCreated() {
        Counter.builder("order.created.total")
                .description("Total number of orders created")
                .register(meterRegistry)
                .increment();
    }

    public void incrementOrderCompleted() {
        Counter.builder("order.completed.total")
                .description("Total number of orders completed")
                .register(meterRegistry)
                .increment();
    }

    public void incrementOrderFailed() {
        Counter.builder("order.failed.total")
                .description("Total number of orders failed")
                .register(meterRegistry)
                .increment();
    }

    // Cache metrics
    public void incrementCacheHit(String cacheName) {
        Counter.builder("cache.hits.total")
                .tag("cache_name", cacheName)
                .description("Total number of cache hits")
                .register(meterRegistry)
                .increment();
    }

    public void incrementCacheMiss(String cacheName) {
        Counter.builder("cache.misses.total")
                .tag("cache_name", cacheName)
                .description("Total number of cache misses")
                .register(meterRegistry)
                .increment();
    }

    // Utility method to time code execution
    public <T> T timeExecution(String operation, TimeSupplier<T> supplier) {
        long startTime = System.nanoTime();
        try {
            return supplier.get();
        } finally {
            long duration = System.nanoTime() - startTime;
            recordProcessingTime(operation, duration, TimeUnit.NANOSECONDS);
        }
    }

    @FunctionalInterface
    public interface TimeSupplier<T> {
        T get();
    }
}