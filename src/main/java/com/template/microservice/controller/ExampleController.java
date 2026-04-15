package com.template.microservice.controller;

import com.template.microservice.model.ExampleRequest;
import com.template.microservice.model.ExampleResponse;
import com.template.microservice.service.ExampleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/example")
@RequiredArgsConstructor
@Tag(name = "Example Controller", description = "Example endpoints for demonstration")
public class ExampleController {

    private final ExampleService exampleService;

    @GetMapping("/{id}")
    @Operation(summary = "Get example by ID")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ExampleResponse> getExample(@PathVariable @NotBlank String id) {
        ExampleResponse response = exampleService.getExample(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Create new example")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExampleResponse> createExample(@Valid @RequestBody ExampleRequest request) {
        ExampleResponse response = exampleService.createExample(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check endpoint")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Service is healthy");
    }
    
    @GetMapping("/public")
    @Operation(summary = "Public endpoint accessible without authentication")
    public ResponseEntity<String> publicEndpoint() {
        return ResponseEntity.ok("This is a public endpoint");
    }
    
    @GetMapping("/resilience/{pattern}")
    @Operation(summary = "Demonstrate resilience patterns: circuitbreaker, retry, bulkhead, ratelimiter")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> demonstrateResiliencePattern(@PathVariable String pattern) {
        String serviceUrl = "http://example.com/api";
        String result;
        
        switch (pattern.toLowerCase()) {
            case "circuitbreaker":
                result = exampleService.callExternalServiceWithCircuitBreaker(serviceUrl);
                break;
            case "retry":
                result = exampleService.callExternalServiceWithRetry(serviceUrl);
                break;
            case "bulkhead":
                result = exampleService.callExternalServiceWithBulkhead(serviceUrl);
                break;
            case "ratelimiter":
                result = exampleService.callExternalServiceWithRateLimiter(serviceUrl);
                break;
            case "async":
                return ResponseEntity.accepted().body("Async call initiated");
            default:
                return ResponseEntity.badRequest().body("Unknown pattern: " + pattern);
        }
        
        return ResponseEntity.ok(result);
    }
}