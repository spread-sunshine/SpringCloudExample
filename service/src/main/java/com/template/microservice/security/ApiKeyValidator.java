package com.template.microservice.security;

/**
 * Validates API keys and returns validation results.
 */
public interface ApiKeyValidator {
    
    /**
     * Validate an API key.
     * @param apiKey The API key to validate
     * @return Validation result
     */
    ApiKeyValidationResult validate(String apiKey);
}