package com.template.microservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyManagementServiceTest {

    private ApiKeyManagementService apiKeyManagementService;
    
    @Mock
    private AuditService auditService;
    
    @Captor
    private ArgumentCaptor<Map<String, Object>> auditDataCaptor;

    @BeforeEach
    void setUp() {
        apiKeyManagementService = new ApiKeyManagementService(auditService);
        ReflectionTestUtils.setField(apiKeyManagementService, "keyExpirationDays", 90);
        ReflectionTestUtils.setField(apiKeyManagementService, "warningDays", 7);
        ReflectionTestUtils.setField(apiKeyManagementService, "keyLength", 32);
        ReflectionTestUtils.setField(apiKeyManagementService, "keyPrefix", "sk_");
    }

    @Test
    void generateApiKey_ShouldReturnValidApiKeyInfo() {
        // Arrange
        String clientName = "Test Client";
        String description = "Test API key";
        List<String> authorities = Arrays.asList("ROLE_USER", "ROLE_API");

        // Act
        ApiKeyManagementService.ApiKeyInfo keyInfo = apiKeyManagementService.generateApiKey(
                clientName, description, authorities);

        // Assert
        assertNotNull(keyInfo);
        assertNotNull(keyInfo.getKey());
        assertTrue(keyInfo.getKey().startsWith("sk_"));
        assertEquals(clientName, keyInfo.getClientName());
        assertEquals(description, keyInfo.getDescription());
        assertEquals(authorities, keyInfo.getAuthorities());
        assertNotNull(keyInfo.getClientId());
        assertNotNull(keyInfo.getCreatedAt());
        assertNotNull(keyInfo.getExpiresAt());
        assertTrue(keyInfo.isActive());
        assertEquals(0, keyInfo.getRotationCount());
        assertNull(keyInfo.getLastUsedAt());
        
        // Verify audit log
        verify(auditService, times(1)).logSensitiveOperation(
                eq("API_KEY_GENERATION"),
                eq(clientName),
                auditDataCaptor.capture());
        
        Map<String, Object> auditData = auditDataCaptor.getValue();
        assertEquals(clientName, auditData.get("clientName"));
        assertEquals(keyInfo.getExpiresAt(), auditData.get("expiresAt"));
        assertEquals(authorities, auditData.get("authorities"));
    }

    @Test
    void validateAndTrackUsage_ShouldReturnValidResult_ForValidKey() {
        // Arrange
        String clientName = "Test Client";
        String description = "Test API key";
        List<String> authorities = Arrays.asList("ROLE_USER");
        
        ApiKeyManagementService.ApiKeyInfo keyInfo = apiKeyManagementService.generateApiKey(
                clientName, description, authorities);
        String apiKey = keyInfo.getKey();

        // Act
        ApiKeyValidationResult result = apiKeyManagementService.validateAndTrackUsage(apiKey);

        // Assert
        assertTrue(result.isValid());
        assertEquals(keyInfo.getClientId(), result.getClientId());
        assertEquals(authorities, result.getAuthorities());
        assertEquals(description, result.getDescription());
        
        // Verify last used timestamp was updated
        assertNotNull(keyInfo.getLastUsedAt());
    }

    @Test
    void validateAndTrackUsage_ShouldReturnInvalid_ForNonExistentKey() {
        // Arrange
        String nonExistentKey = "sk_nonexistentkey1234567890";

        // Act
        ApiKeyValidationResult result = apiKeyManagementService.validateAndTrackUsage(nonExistentKey);

        // Assert
        assertFalse(result.isValid());
        assertNull(result.getClientId());
        assertNull(result.getAuthorities());
        assertNull(result.getDescription());
    }

    @Test
    void validateAndTrackUsage_ShouldReturnInvalid_ForInactiveKey() {
        // Arrange
        ApiKeyManagementService.ApiKeyInfo keyInfo = apiKeyManagementService.generateApiKey(
                "Test", "Test", List.of("ROLE_USER"));
        keyInfo.setActive(false);
        String apiKey = keyInfo.getKey();

        // Act
        ApiKeyValidationResult result = apiKeyManagementService.validateAndTrackUsage(apiKey);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Key is inactive", result.getDescription());
    }

    @Test
    void validateAndTrackUsage_ShouldDeactivateAndReturnInvalid_ForExpiredKey() {
        // Arrange
        ApiKeyManagementService.ApiKeyInfo keyInfo = apiKeyManagementService.generateApiKey(
                "Test", "Test", List.of("ROLE_USER"));
        keyInfo.setExpiresAt(LocalDateTime.now().minusDays(1));
        String apiKey = keyInfo.getKey();

        // Act
        ApiKeyValidationResult result = apiKeyManagementService.validateAndTrackUsage(apiKey);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Key has expired", result.getDescription());
        assertFalse(keyInfo.isActive());
        assertNotNull(keyInfo.getDeactivatedAt());
        assertEquals("Expired", keyInfo.getDeactivationReason());
    }

    @Test
    void rotateApiKey_ShouldGenerateNewKeyAndDeactivateOld() {
        // Arrange
        ApiKeyManagementService.ApiKeyInfo oldKeyInfo = apiKeyManagementService.generateApiKey(
                "Test Client", "Original key", List.of("ROLE_USER"));
        String oldApiKey = oldKeyInfo.getKey();
        String rotationReason = "Security rotation";

        // Act
        ApiKeyManagementService.ApiKeyInfo newKeyInfo = apiKeyManagementService.rotateApiKey(
                oldApiKey, rotationReason);

        // Assert
        assertNotNull(newKeyInfo);
        assertNotEquals(oldApiKey, newKeyInfo.getKey());
        assertEquals(oldKeyInfo.getClientId(), newKeyInfo.getClientId());
        assertEquals(oldKeyInfo.getClientName(), newKeyInfo.getClientName());
        assertEquals(oldKeyInfo.getAuthorities(), newKeyInfo.getAuthorities());
        assertEquals(oldKeyInfo.getRotationCount() + 1, newKeyInfo.getRotationCount());
        assertTrue(newKeyInfo.getDescription().contains(rotationReason));
        
        // Verify old key is deactivated
        assertFalse(oldKeyInfo.isActive());
        assertNotNull(oldKeyInfo.getDeactivatedAt());
        assertTrue(oldKeyInfo.getDeactivationReason().contains(rotationReason));
        
        // Verify audit log
        verify(auditService, times(2)).logSensitiveOperation(
                anyString(),
                anyString(),
                anyMap());
    }

    @Test
    void rotateApiKey_ShouldThrowException_ForInvalidKey() {
        // Arrange
        String invalidKey = "invalid_key";
        String rotationReason = "Security rotation";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> apiKeyManagementService.rotateApiKey(invalidKey, rotationReason));
        assertEquals("Invalid or inactive API key", exception.getMessage());
    }

    @Test
    void revokeApiKey_ShouldDeactivateKey() {
        // Arrange
        ApiKeyManagementService.ApiKeyInfo keyInfo = apiKeyManagementService.generateApiKey(
                "Test Client", "Test", List.of("ROLE_USER"));
        String apiKey = keyInfo.getKey();
        String revocationReason = "Security breach";

        // Act
        boolean revoked = apiKeyManagementService.revokeApiKey(apiKey, revocationReason);

        // Assert
        assertTrue(revoked);
        assertFalse(keyInfo.isActive());
        assertNotNull(keyInfo.getDeactivatedAt());
        assertEquals(revocationReason, keyInfo.getDeactivationReason());
        
        // Verify audit log
        verify(auditService, times(2)).logSensitiveOperation(
                anyString(),
                anyString(),
                anyMap());
    }

    @Test
    void revokeApiKey_ShouldReturnFalse_ForNonExistentKey() {
        // Arrange
        String nonExistentKey = "sk_nonexistent";

        // Act
        boolean revoked = apiKeyManagementService.revokeApiKey(nonExistentKey, "test");

        // Assert
        assertFalse(revoked);
    }

    @Test
    void listActiveApiKeys_ShouldReturnOnlyActiveKeys() {
        // Arrange
        ApiKeyManagementService.ApiKeyInfo activeKey1 = apiKeyManagementService.generateApiKey(
                "Client1", "Test1", List.of("ROLE_USER"));
        ApiKeyManagementService.ApiKeyInfo activeKey2 = apiKeyManagementService.generateApiKey(
                "Client2", "Test2", List.of("ROLE_ADMIN"));
        ApiKeyManagementService.ApiKeyInfo inactiveKey = apiKeyManagementService.generateApiKey(
                "Client3", "Test3", List.of("ROLE_USER"));
        inactiveKey.setActive(false);

        // Act
        List<ApiKeyManagementService.ApiKeyInfo> activeKeys = apiKeyManagementService.listActiveApiKeys();

        // Assert
        assertEquals(2, activeKeys.size());
        assertTrue(activeKeys.stream().allMatch(ApiKeyManagementService.ApiKeyInfo::isActive));
        assertTrue(activeKeys.stream().anyMatch(k -> k.getClientName().equals("Client1")));
        assertTrue(activeKeys.stream().anyMatch(k -> k.getClientName().equals("Client2")));
    }

    @Test
    void getApiKeyInfo_ShouldReturnMaskedCopy() {
        // Arrange
        ApiKeyManagementService.ApiKeyInfo original = apiKeyManagementService.generateApiKey(
                "Test Client", "Test", List.of("ROLE_USER"));
        String clientId = original.getClientId();

        // Act
        Optional<ApiKeyManagementService.ApiKeyInfo> result = apiKeyManagementService.getApiKeyInfo(clientId);

        // Assert
        assertTrue(result.isPresent());
        ApiKeyManagementService.ApiKeyInfo masked = result.get();
        
        // Should not expose the actual key
        assertNull(masked.getKey());
        
        // Other properties should be the same
        assertEquals(original.getClientId(), masked.getClientId());
        assertEquals(original.getClientName(), masked.getClientName());
        assertEquals(original.getDescription(), masked.getDescription());
        assertEquals(original.getAuthorities(), masked.getAuthorities());
        assertEquals(original.getCreatedAt(), masked.getCreatedAt());
        assertEquals(original.getExpiresAt(), masked.getExpiresAt());
        assertEquals(original.getLastUsedAt(), masked.getLastUsedAt());
        assertEquals(original.isActive(), masked.isActive());
        assertEquals(original.getRotationCount(), masked.getRotationCount());
    }

    @Test
    void getApiKeyInfo_ShouldReturnEmpty_ForNonExistentClient() {
        // Arrange
        String nonExistentClientId = "non-existent";

        // Act
        Optional<ApiKeyManagementService.ApiKeyInfo> result = apiKeyManagementService.getApiKeyInfo(nonExistentClientId);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void checkExpiringKeys_ShouldNotThrowException() {
        // Act & Assert (should not throw)
        assertDoesNotThrow(() -> 
            ReflectionTestUtils.invokeMethod(apiKeyManagementService, "checkExpiringKeys")
        );
    }

    @Test
    void cleanupOldKeys_ShouldNotThrowException() {
        // Act & Assert (should not throw)
        assertDoesNotThrow(() -> 
            ReflectionTestUtils.invokeMethod(apiKeyManagementService, "cleanupOldKeys")
        );
    }

    @Test
    void generateSecureApiKey_ShouldGenerateValidKey() {
        // Act
        String key = (String) ReflectionTestUtils.invokeMethod(
                apiKeyManagementService, "generateSecureApiKey");

        // Assert
        assertNotNull(key);
        assertTrue(key.startsWith("sk_"));
        assertTrue(key.length() > 32); // Prefix + base64 encoded bytes
    }

    @Test
    void maskKey_ShouldHideMiddlePartOfKey() {
        // Arrange
        String apiKey = "sk_abcdefghijklmnopqrstuvwxyz123456";

        // Act
        String masked = (String) ReflectionTestUtils.invokeMethod(
                apiKeyManagementService, "maskKey", apiKey);

        // Assert - 验证核心行为而非具体格式
        assertTrue(masked.startsWith("sk_"), "应保留前缀");
        assertTrue(masked.endsWith("3456"), "应保留最后4个字符");
        assertTrue(masked.contains("..."), "中间部分应被掩码");
        assertTrue(masked.length() < apiKey.length(), "掩码后长度应缩短");
    }

    @Test
    void maskKey_ShouldFullyMaskVeryShortKeys() {
        // Arrange
        String shortKey = "sk_short";

        // Act
        String masked = (String) ReflectionTestUtils.invokeMethod(
                apiKeyManagementService, "maskKey", shortKey);

        // Assert - 验证短密钥被完全掩码
        assertEquals("***", masked);
        // 或更灵活的断言：
        // assertNotEquals(shortKey, masked, "短密钥应被掩码");
        // assertTrue(masked.length() <= 5, "掩码后长度应较短");
    }
    
    @Test
    void maskKey_ShouldHandleNullAndEmpty() {
        // Arrange
        String nullKey = null;
        String emptyKey = "";
        
        // Act & Assert
        // 根据实现，对于null或长度<=8的密钥，返回"***"
        assertEquals("***", (String) ReflectionTestUtils.invokeMethod(
                apiKeyManagementService, "maskKey", nullKey));
        assertEquals("***", (String) ReflectionTestUtils.invokeMethod(
                apiKeyManagementService, "maskKey", emptyKey));
    }

    @Test
    void apiKeyInfoBuilder_ShouldCreateValidObject() {
        // Arrange & Act
        ApiKeyManagementService.ApiKeyInfo keyInfo = ApiKeyManagementService.ApiKeyInfo.builder()
                .key("sk_testkey")
                .keyHash("hash123")
                .clientId("client-123")
                .clientName("Test Client")
                .description("Test key")
                .authorities(List.of("ROLE_USER"))
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(90))
                .lastUsedAt(LocalDateTime.now())
                .isActive(true)
                .rotationCount(0)
                .build();

        // Assert
        assertEquals("sk_testkey", keyInfo.getKey());
        assertEquals("hash123", keyInfo.getKeyHash());
        assertEquals("client-123", keyInfo.getClientId());
        assertEquals("Test Client", keyInfo.getClientName());
        assertEquals("Test key", keyInfo.getDescription());
        assertEquals(1, keyInfo.getAuthorities().size());
        assertEquals("ROLE_USER", keyInfo.getAuthorities().get(0));
        assertEquals(0, keyInfo.getRotationCount());
    }
}