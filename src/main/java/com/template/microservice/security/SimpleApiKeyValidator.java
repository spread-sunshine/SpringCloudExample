package com.template.microservice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Simple API key validator that validates against configured API keys.
 * For production use, this should be replaced with a database-backed validator.
 */
@Component
@ConditionalOnProperty(name = "security.api.validator", havingValue = "simple")
@Slf4j
public class SimpleApiKeyValidator implements ApiKeyValidator {
    
    /**
     * Configured API keys in format: key1:ROLE_USER,ROLE_ADMIN:Client 1;key2:ROLE_USER:Client 2
     */
    @Value("${security.api.keys:}")
    private String apiKeysConfig;
    
    private Map<String, ApiKeyConfig> apiKeyConfigMap;
    
    @Override
    public ApiKeyValidationResult validate(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return ApiKeyValidationResult.invalid();
        }
        
        initializeConfig();
        
        ApiKeyConfig config = apiKeyConfigMap.get(apiKey.trim());
        if (config != null) {
            return ApiKeyValidationResult.valid(
                    config.getClientId(),
                    config.getAuthorities(),
                    config.getDescription()
            );
        }
        
        log.warn("Invalid API key attempted: {}", maskApiKey(apiKey));
        return ApiKeyValidationResult.invalid();
    }
    
    private void initializeConfig() {
        if (apiKeyConfigMap != null) {
            return;
        }
        
        synchronized (this) {
            if (apiKeyConfigMap != null) {
                return;
            }
            
            apiKeyConfigMap = parseApiKeysConfig();
            log.info("Loaded {} API key configurations", apiKeyConfigMap.size());
        }
    }
    
    private Map<String, ApiKeyConfig> parseApiKeysConfig() {
        if (apiKeysConfig == null || apiKeysConfig.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        
        return Arrays.stream(apiKeysConfig.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::parseSingleApiKeyConfig)
                .collect(Collectors.toMap(ApiKeyConfig::getKey, config -> config));
    }
    
    private ApiKeyConfig parseSingleApiKeyConfig(String configStr) {
        String[] parts = configStr.split(":", 3);
        String key = parts[0].trim();
        String authoritiesStr = parts.length > 1 ? parts[1].trim() : "ROLE_USER";
        String description = parts.length > 2 ? parts[2].trim() : "API Client";
        
        List<String> authorities = Arrays.stream(authoritiesStr.split(","))
                .map(String::trim)
                .filter(a -> !a.isEmpty())
                .collect(Collectors.toList());
        
        if (authorities.isEmpty()) {
            authorities = Collections.singletonList("ROLE_USER");
        }
        
        return new ApiKeyConfig(key, "client-" + key.hashCode(), authorities, description);
    }
    
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "***";
        }
        return apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
    }
    
    @lombok.Value
    private static class ApiKeyConfig {
        String key;
        String clientId;
        List<String> authorities;
        String description;
    }
}