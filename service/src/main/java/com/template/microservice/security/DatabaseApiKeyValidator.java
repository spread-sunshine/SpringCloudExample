package com.template.microservice.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Database-backed API key validator that uses ApiKeyManagementService.
 * This is the recommended implementation for production use.
 */
@Component
@ConditionalOnProperty(name = "security.api.validator", havingValue = "database", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class DatabaseApiKeyValidator implements ApiKeyValidator {

    private final ApiKeyManagementService apiKeyManagementService;
    private final AuditService auditService;

    @Override
    public ApiKeyValidationResult validate(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return ApiKeyValidationResult.invalid();
        }

        String trimmedKey = apiKey.trim();
        
        try {
            ApiKeyValidationResult result = apiKeyManagementService.validateAndTrackUsage(trimmedKey);
            
            // Log audit event
            if (result.isValid()) {
                auditService.logApiKeyUsage(
                        result.getClientId(),
                        hashKey(trimmedKey),
                        true,
                        null // Request would be passed in a real implementation
                );
                log.debug("Valid API key for client: {}", result.getClientId());
            } else {
                auditService.logApiKeyUsage(
                        "UNKNOWN",
                        hashKey(trimmedKey),
                        false,
                        null
                );
                log.warn("Invalid API key attempted: {}", maskApiKey(trimmedKey));
            }
            
            return result;
        } catch (Exception ex) {
            log.error("Error validating API key", ex);
            return ApiKeyValidationResult.invalid("Validation error");
        }
    }

    private String hashKey(String apiKey) {
        // Simple hash for logging - in production use proper cryptographic hash
        return Integer.toHexString(apiKey.hashCode());
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "***";
        }
        return apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
    }
}