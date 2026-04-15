package com.template.microservice.security;

import com.template.microservice.constants.SecurityConstants;
import com.template.microservice.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing API keys with rotation and expiration support.
 * For production, this should be backed by a database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyManagementService {

    private final AuditService auditService;
    
    @Value("${security.api.key.expiration.days:90}")
    private int keyExpirationDays;
    
    @Value("${security.api.key.warning.days:7}")
    private int warningDays;
    
    @Value("${security.api.key.length:32}")
    private int keyLength;
    
    @Value("${security.api.key.prefix:sk_}")
    private String keyPrefix;
    
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, ApiKeyInfo> apiKeyStore = new ConcurrentHashMap<>();
    
    /**
     * Generate a new API key for a client.
     */
    public ApiKeyInfo generateApiKey(String clientName, String description, List<String> authorities) {
        String apiKey = generateSecureApiKey();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(keyExpirationDays);
        
        ApiKeyInfo keyInfo = ApiKeyInfo.builder()
                .key(apiKey)
                .keyHash(hashKey(apiKey))
                .clientId(UUID.randomUUID().toString())
                .clientName(clientName)
                .description(description)
                .authorities(new ArrayList<>(authorities))
                .createdAt(now)
                .expiresAt(expiresAt)
                .lastUsedAt(null)
                .isActive(true)
                .rotationCount(0)
                .build();
        
        apiKeyStore.put(apiKey, keyInfo);
        
        log.info("Generated API key for client: {}, expires: {}", clientName, expiresAt);
        
        // Audit log
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("clientName", clientName);
        auditData.put("expiresAt", expiresAt);
        auditData.put("authorities", authorities);
        auditService.logSensitiveOperation(SecurityConstants.AUDIT_API_KEY_GENERATION, clientName, auditData);
        
        return keyInfo;
    }
    
    /**
     * Rotate an existing API key.
     */
    public ApiKeyInfo rotateApiKey(String oldApiKey, String reason) {
        ApiKeyInfo oldKeyInfo = apiKeyStore.get(oldApiKey);
        if (oldKeyInfo == null || !oldKeyInfo.isActive()) {
            throw new IllegalArgumentException("Invalid or inactive API key");
        }
        
        // Generate new key
        String newApiKey = generateSecureApiKey();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(keyExpirationDays);
        
        // Create new key info based on old key
        ApiKeyInfo newKeyInfo = ApiKeyInfo.builder()
                .key(newApiKey)
                .keyHash(hashKey(newApiKey))
                .clientId(oldKeyInfo.getClientId())
                .clientName(oldKeyInfo.getClientName())
                .description(oldKeyInfo.getDescription() + " (rotated: " + reason + ")")
                .authorities(new ArrayList<>(oldKeyInfo.getAuthorities()))
                .createdAt(now)
                .expiresAt(expiresAt)
                .lastUsedAt(null)
                .isActive(true)
                .rotationCount(oldKeyInfo.getRotationCount() + 1)
                .build();
        
        // Deactivate old key
        oldKeyInfo.setActive(false);
        oldKeyInfo.setDeactivatedAt(now);
        oldKeyInfo.setDeactivationReason("Rotated to new key: " + reason);
        
        // Store new key
        apiKeyStore.put(newApiKey, newKeyInfo);
        
        log.info("Rotated API key for client: {}, old key deactivated", oldKeyInfo.getClientName());
        
        // Audit log
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("reason", reason);
        auditData.put("oldKeyHash", oldKeyInfo.getKeyHash());
        auditData.put("newKeyHash", newKeyInfo.getKeyHash());
        auditService.logSensitiveOperation(SecurityConstants.AUDIT_API_KEY_ROTATION, oldKeyInfo.getClientName(), auditData);
        
        return newKeyInfo;
    }
    
    /**
     * Validate and update usage for an API key.
     */
    public ApiKeyValidationResult validateAndTrackUsage(String apiKey) {
        ApiKeyInfo keyInfo = apiKeyStore.get(apiKey);
        
        if (keyInfo == null) {
            return ApiKeyValidationResult.invalid();
        }
        
        if (!keyInfo.isActive()) {
            log.warn("Inactive API key attempted: {}", maskKey(keyInfo.getKey()));
            return ApiKeyValidationResult.invalid(SecurityConstants.INVALID_KEY_MESSAGE_INACTIVE);
        }
        
        if (keyInfo.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Expired API key attempted: {}", maskKey(keyInfo.getKey()));
            keyInfo.setActive(false);
            keyInfo.setDeactivatedAt(LocalDateTime.now());
            keyInfo.setDeactivationReason(SecurityConstants.DEACTIVATION_REASON_EXPIRED);
            return ApiKeyValidationResult.invalid(SecurityConstants.INVALID_KEY_MESSAGE_EXPIRED);
        }
        
        // Update last used timestamp
        keyInfo.setLastUsedAt(LocalDateTime.now());
        
        return ApiKeyValidationResult.valid(
                keyInfo.getClientId(),
                keyInfo.getAuthorities(),
                keyInfo.getDescription()
        );
    }
    
    /**
     * Revoke an API key.
     */
    public boolean revokeApiKey(String apiKey, String reason) {
        ApiKeyInfo keyInfo = apiKeyStore.get(apiKey);
        if (keyInfo == null) {
            return false;
        }
        
        keyInfo.setActive(false);
        keyInfo.setDeactivatedAt(LocalDateTime.now());
        keyInfo.setDeactivationReason(reason);
        
        log.info("Revoked API key for client: {}, reason: {}", keyInfo.getClientName(), reason);
        
        // Audit log
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("reason", reason);
        auditData.put("keyHash", keyInfo.getKeyHash());
        auditService.logSensitiveOperation(SecurityConstants.AUDIT_API_KEY_REVOCATION, keyInfo.getClientName(), auditData);
        
        return true;
    }
    
    /**
     * List all active API keys.
     */
    public List<ApiKeyInfo> listActiveApiKeys() {
        return apiKeyStore.values().stream()
                .filter(ApiKeyInfo::isActive)
                .sorted(Comparator.comparing(ApiKeyInfo::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * Get API key information (without the actual key).
     */
    public Optional<ApiKeyInfo> getApiKeyInfo(String clientId) {
        return apiKeyStore.values().stream()
                .filter(keyInfo -> keyInfo.getClientId().equals(clientId) && keyInfo.isActive())
                .findFirst()
                .map(this::createMaskedCopy);
    }
    
    /**
     * Check for expiring keys and send notifications.
     */
    @Scheduled(cron = "0 0 8 * * ?") // Daily at 8 AM
    public void checkExpiringKeys() {
        LocalDateTime warningThreshold = LocalDateTime.now().plusDays(warningDays);
        
        List<ApiKeyInfo> expiringKeys = apiKeyStore.values().stream()
                .filter(ApiKeyInfo::isActive)
                .filter(keyInfo -> keyInfo.getExpiresAt().isBefore(warningThreshold))
                .collect(Collectors.toList());
        
        if (!expiringKeys.isEmpty()) {
            log.warn("Found {} API keys expiring within {} days", expiringKeys.size(), warningDays);
            expiringKeys.forEach(keyInfo -> 
                log.warn("Key for client '{}' expires on {}", 
                        keyInfo.getClientName(), keyInfo.getExpiresAt()));
            
            // In production, send email or Slack notifications here
        }
    }
    
    /**
     * Clean up expired and revoked keys (older than 30 days).
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void cleanupOldKeys() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        
        List<String> keysToRemove = apiKeyStore.entrySet().stream()
                .filter(entry -> !entry.getValue().isActive())
                .filter(entry -> entry.getValue().getDeactivatedAt() != null &&
                               entry.getValue().getDeactivatedAt().isBefore(cutoffDate))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        if (!keysToRemove.isEmpty()) {
            keysToRemove.forEach(apiKeyStore::remove);
            log.info("Cleaned up {} old API keys", keysToRemove.size());
        }
    }
    
    private String generateSecureApiKey() {
        byte[] randomBytes = new byte[keyLength];
        secureRandom.nextBytes(randomBytes);
        
        // Convert to base64 and remove non-alphanumeric characters
        String base64Key = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return keyPrefix + base64Key;
    }
    
    private String hashKey(String apiKey) {
        return SecurityUtils.computeSha256Hash(apiKey);
    }
    
    private String maskKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "***";
        }
        return apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
    }
    
    private ApiKeyInfo createMaskedCopy(ApiKeyInfo original) {
        // Create a copy without the actual API key for security
        return ApiKeyInfo.builder()
                .key(null) // Don't expose the key
                .keyHash(original.getKeyHash())
                .clientId(original.getClientId())
                .clientName(original.getClientName())
                .description(original.getDescription())
                .authorities(new ArrayList<>(original.getAuthorities()))
                .createdAt(original.getCreatedAt())
                .expiresAt(original.getExpiresAt())
                .lastUsedAt(original.getLastUsedAt())
                .isActive(original.isActive())
                .rotationCount(original.getRotationCount())
                .build();
    }
    
    /**
     * API key information.
     */
    @lombok.Builder
    @lombok.Data
    public static class ApiKeyInfo {
        private String key; // The actual API key (only shown on generation)
        private String keyHash; // Hash of the API key for verification
        private String clientId;
        private String clientName;
        private String description;
        private List<String> authorities;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;
        private LocalDateTime lastUsedAt;
        private boolean isActive;
        private Integer rotationCount;
        private LocalDateTime deactivatedAt;
        private String deactivationReason;
    }
}