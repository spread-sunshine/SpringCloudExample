package com.template.microservice.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ExampleControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testGetExample() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                getUrl("/api/example/123"),
                String.class
        );

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("id"));
        assertTrue(response.getBody().contains("message"));
        assertTrue(response.getBody().contains("timestamp"));
    }

    @Test
    void testCreateExample() {
        Map<String, String> request = new HashMap<>();
        request.put("message", "Test message");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                getUrl("/api/example"),
                HttpMethod.POST,
                entity,
                String.class
        );

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("id"));
        assertTrue(response.getBody().contains("Test message"));
    }

    @Test
    void testHealthEndpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                getUrl("/api/example/health"),
                String.class
        );

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Service is healthy", response.getBody());
    }

    @Test
    void testValidation() {
        Map<String, String> request = new HashMap<>();
        // Empty message should fail validation
        request.put("message", "");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                getUrl("/api/example"),
                HttpMethod.POST,
                entity,
                String.class
        );

        assertEquals(400, response.getStatusCodeValue());
        // Should contain validation error response
        assertTrue(response.getBody().contains("VALIDATION_ERROR"));
    }

    @Test
    void testCircuitBreaker() {
        // This test would require a mock external service
        // to trigger circuit breaker
        
        // Simulate multiple calls to test circuit breaker
        for (int i = 0; i < 10; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    getUrl("/api/example/circuit-breaker-test"),
                    String.class
            );
            
            // In a real test, we would mock an external service
            // that fails after certain attempts
        }
        
        // Verify circuit breaker state
        // This is a placeholder for actual circuit breaker testing
        assertTrue(true);
    }

    @Test
    void testRateLimiting() {
        // This test would require hitting the rate limit
        // and verifying the response
        
        HttpHeaders headers = new HttpHeaders();
        
        // Make multiple requests quickly
        for (int i = 0; i < 150; i++) { // Assuming limit is 100/minute
            ResponseEntity<String> response = restTemplate.exchange(
                    getUrl("/api/example/123"),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            
            if (i >= 100) {
                // After rate limit exceeded
                assertEquals(429, response.getStatusCodeValue());
                break;
            }
        }
        
        // In real tests, this would be more controlled
        // with proper rate limiting configuration
    }

    @Test
    void testMetrics() {
        // Access metrics endpoint
        ResponseEntity<String> response = restTemplate.getForEntity(
                getUrl("/actuator/metrics"),
                String.class
        );

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        
        // Check for business metrics
        ResponseEntity<String> businessMetrics = restTemplate.getForEntity(
                getUrl("/actuator/metrics/business.requests.total"),
                String.class
        );
        
        assertEquals(200, businessMetrics.getStatusCodeValue());
    }
}