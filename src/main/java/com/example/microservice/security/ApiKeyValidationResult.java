package com.example.microservice.security;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Result of API key validation.
 */
@Getter
@Builder
public class ApiKeyValidationResult {
    
    private final boolean valid;
    private final String clientId;
    private final List<String> authorities;
    private final String description;
    
    public static ApiKeyValidationResult valid(String clientId, List<String> authorities, String description) {
        return ApiKeyValidationResult.builder()
                .valid(true)
                .clientId(clientId)
                .authorities(authorities)
                .description(description)
                .build();
    }
    
    public static ApiKeyValidationResult invalid() {
        return ApiKeyValidationResult.builder()
                .valid(false)
                .build();
    }
}