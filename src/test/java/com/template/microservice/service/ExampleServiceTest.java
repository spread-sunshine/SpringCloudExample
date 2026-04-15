package com.template.microservice.service;

import com.template.microservice.model.ExampleRequest;
import com.template.microservice.model.ExampleResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ExampleServiceTest {

    @Autowired
    private ExampleService exampleService;

    @Test
    void getExample_ShouldReturnValidResponse() {
        // Arrange
        String id = "test-123";

        // Act
        ExampleResponse response = exampleService.getExample(id);

        // Assert
        assertNotNull(response);
        assertEquals(id, response.getId());
        assertEquals("Hello from microservice template", response.getMessage());
        assertTrue(response.getTimestamp() > 0);
    }

    @Test
    void getExample_ShouldReturnDifferentTimestamps() {
        // Arrange
        String id = "test-123";

        // Act
        ExampleResponse response1 = exampleService.getExample(id);
        try {
            TimeUnit.MILLISECONDS.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        ExampleResponse response2 = exampleService.getExample(id);

        // Assert
        assertNotEquals(response1.getTimestamp(), response2.getTimestamp());
    }

    @Test
    void createExample_ShouldReturnValidResponse() {
        // Arrange
        ExampleRequest request = ExampleRequest.builder()
                .message("Test message")
                .build();

        // Act
        ExampleResponse response = exampleService.createExample(request);

        // Assert
        assertNotNull(response);
        assertTrue(response.getId().startsWith("generated-"));
        assertEquals("Created: Test message", response.getMessage());
        assertTrue(response.getTimestamp() > 0);
    }

    @Test
    void callExternalServiceWithCircuitBreaker_ShouldReturnResponseOrFallback() {
        // Arrange
        String serviceUrl = "http://example.com/api";

        // Act & Assert - this test mainly verifies the method doesn't throw
        // Since it has random failure logic, we test it multiple times
        for (int i = 0; i < 10; i++) {
            String result = exampleService.callExternalServiceWithCircuitBreaker(serviceUrl);
            assertNotNull(result);
            assertTrue(result.contains(serviceUrl) || result.contains("Fallback"));
        }
    }

    @Test
    void callExternalServiceWithRetry_ShouldReturnResponseOrFallback() {
        // Arrange
        String serviceUrl = "http://example.com/api";

        // Act & Assert
        for (int i = 0; i < 10; i++) {
            String result = exampleService.callExternalServiceWithRetry(serviceUrl);
            assertNotNull(result);
            assertTrue(result.contains(serviceUrl) || result.contains("Fallback"));
        }
    }

    @Test
    void callExternalServiceWithBulkhead_ShouldReturnResponseOrFallback() {
        // Arrange
        String serviceUrl = "http://example.com/api";

        // Act
        String result = exampleService.callExternalServiceWithBulkhead(serviceUrl);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains(serviceUrl) || result.contains("Fallback"));
    }

    @Test
    void callExternalServiceWithRateLimiter_ShouldReturnResponseOrFallback() {
        // Arrange
        String serviceUrl = "http://example.com/api";

        // Act
        String result = exampleService.callExternalServiceWithRateLimiter(serviceUrl);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains(serviceUrl) || result.contains("Fallback"));
    }

    @Test
    void callExternalServiceAsync_ShouldReturnFuture() throws Exception {
        // Arrange
        String serviceUrl = "http://example.com/api";

        // Act
        CompletableFuture<String> future = exampleService.callExternalServiceAsync(serviceUrl);

        // Assert
        assertNotNull(future);
        String result = future.get(2, TimeUnit.SECONDS);
        assertNotNull(result);
        assertTrue(result.contains(serviceUrl) || result.contains("Fallback"));
    }

    @Test
    void fallbackForExternalService_ShouldReturnFallbackResponse() {
        // Arrange
        String serviceUrl = "http://example.com/api";
        Throwable throwable = new RuntimeException("Service unavailable");

        // Act
        String result = exampleService.fallbackForExternalService(serviceUrl, throwable);

        // Assert
        assertEquals("Fallback response for " + serviceUrl, result);
    }

    @Test
    void fallbackForExternalServiceAsync_ShouldReturnCompletedFuture() throws Exception {
        // Arrange
        String serviceUrl = "http://example.com/api";
        Throwable throwable = new RuntimeException("Service unavailable");

        // Act
        CompletableFuture<String> future = exampleService.fallbackForExternalServiceAsync(serviceUrl, throwable);

        // Assert
        assertNotNull(future);
        assertTrue(future.isDone());
        String result = future.get();
        assertEquals("Async fallback for " + serviceUrl, result);
    }
}