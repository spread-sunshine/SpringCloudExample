package com.example.microservice.service;

import com.example.microservice.model.ExampleRequest;
import com.example.microservice.model.ExampleResponse;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ExampleService {

    public ExampleResponse getExample(String id) {
        log.info("Fetching example with id: {}", id);
        // Simulate business logic
        return ExampleResponse.builder()
                .id(id)
                .message("Hello from microservice template")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public ExampleResponse createExample(ExampleRequest request) {
        log.info("Creating example with data: {}", request);
        // Simulate creation
        return ExampleResponse.builder()
                .id("generated-" + System.currentTimeMillis())
                .message("Created: " + request.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * Example method using Circuit Breaker pattern.
     * The circuit breaker configuration is defined in application.yml under resilience4j.circuitbreaker.instances.backendService
     */
    @CircuitBreaker(name = "backendService", fallbackMethod = "fallbackForExternalService")
    public String callExternalServiceWithCircuitBreaker(String serviceUrl) {
        log.info("Calling external service: {}", serviceUrl);
        // Simulate external service call that may fail
        if (Math.random() > 0.7) {
            throw new RuntimeException("External service unavailable");
        }
        return "Response from " + serviceUrl;
    }
    
    /**
     * Example method using Retry pattern.
     * The retry configuration is defined in application.yml under resilience4j.retry.instances.backendService
     */
    @Retry(name = "backendService", fallbackMethod = "fallbackForExternalService")
    public String callExternalServiceWithRetry(String serviceUrl) {
        log.info("Calling external service with retry: {}", serviceUrl);
        // Simulate occasional failures
        if (Math.random() > 0.8) {
            throw new RuntimeException("Temporary service failure");
        }
        return "Response from " + serviceUrl;
    }
    
    /**
     * Example method using Bulkhead pattern.
     * The bulkhead configuration is defined in application.yml under resilience4j.bulkhead.instances.backendService
     */
    @Bulkhead(name = "backendService", type = Bulkhead.Type.SEMAPHORE, fallbackMethod = "fallbackForExternalService")
    public String callExternalServiceWithBulkhead(String serviceUrl) {
        log.info("Calling external service with bulkhead protection: {}", serviceUrl);
        // Simulate processing time
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Response from " + serviceUrl;
    }
    
    /**
     * Example method using Rate Limiter pattern.
     * The rate limiter configuration is defined in application.yml under resilience4j.ratelimiter.instances.apiRateLimiter
     */
    @RateLimiter(name = "apiRateLimiter", fallbackMethod = "fallbackForExternalService")
    public String callExternalServiceWithRateLimiter(String serviceUrl) {
        log.info("Calling external service with rate limiting: {}", serviceUrl);
        return "Response from " + serviceUrl;
    }
    
    /**
     * Asynchronous bulkhead example.
     */
    @Bulkhead(name = "backendService", type = Bulkhead.Type.THREADPOOL, fallbackMethod = "fallbackForExternalServiceAsync")
    public CompletableFuture<String> callExternalServiceAsync(String serviceUrl) {
        log.info("Calling external service asynchronously: {}", serviceUrl);
        return CompletableFuture.supplyAsync(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Async response from " + serviceUrl;
        });
    }
    
    /**
     * Fallback method for circuit breaker, retry, bulkhead, and rate limiter.
     */
    public String fallbackForExternalService(String serviceUrl, Throwable t) {
        log.warn("Fallback triggered for service: {}, error: {}", serviceUrl, t.getMessage());
        return "Fallback response for " + serviceUrl;
    }
    
    /**
     * Fallback method for asynchronous calls.
     */
    public CompletableFuture<String> fallbackForExternalServiceAsync(String serviceUrl, Throwable t) {
        log.warn("Async fallback triggered for service: {}, error: {}", serviceUrl, t.getMessage());
        return CompletableFuture.completedFuture("Async fallback for " + serviceUrl);
    }
}