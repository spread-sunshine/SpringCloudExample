package com.template.microservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseApiKeyValidatorTest {

    private DatabaseApiKeyValidator validator;
    
    @Mock
    private ApiKeyManagementService apiKeyManagementService;
    
    @Mock
    private AuditService auditService;
    
    @Captor
    private ArgumentCaptor<String> clientIdCaptor;
    
    @Captor
    private ArgumentCaptor<String> apiKeyHashCaptor;
    
    @Captor
    private ArgumentCaptor<Boolean> successCaptor;

    @BeforeEach
    void setUp() {
        validator = new DatabaseApiKeyValidator(apiKeyManagementService, auditService);
    }

    @Test
    void validate_ShouldReturnValidResult_ForValidApiKey() {
        // Arrange
        String apiKey = "sk_validapikey1234567890abcdef";
        String clientId = "client-123";
        String description = "Test client";
        var authorities = Arrays.asList("ROLE_USER", "ROLE_API");
        
        ApiKeyValidationResult validationResult = ApiKeyValidationResult.valid(
                clientId, authorities, description);
        
        when(apiKeyManagementService.validateAndTrackUsage(apiKey))
                .thenReturn(validationResult);

        // Act
        ApiKeyValidationResult result = validator.validate(apiKey);

        // Assert
        assertTrue(result.isValid());
        assertEquals(clientId, result.getClientId());
        assertEquals(authorities, result.getAuthorities());
        assertEquals(description, result.getDescription());
        
        // Verify audit log
        verify(auditService, times(1)).logApiKeyUsage(
                eq(clientId),
                anyString(),
                eq(true),
                isNull());
    }

    @Test
    void validate_ShouldReturnInvalidResult_ForInvalidApiKey() {
        // Arrange
        String apiKey = "sk_invalidapikey1234567890";
        
        when(apiKeyManagementService.validateAndTrackUsage(apiKey))
                .thenReturn(ApiKeyValidationResult.invalid("Invalid key"));

        // Act
        ApiKeyValidationResult result = validator.validate(apiKey);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Invalid key", result.getDescription());
        
        // Verify audit log
        verify(auditService, times(1)).logApiKeyUsage(
                eq("UNKNOWN"),
                anyString(),
                eq(false),
                isNull());
    }

    @Test
    void validate_ShouldReturnInvalid_ForNullApiKey() {
        // Arrange
        String apiKey = null;

        // Act
        ApiKeyValidationResult result = validator.validate(apiKey);

        // Assert
        assertFalse(result.isValid());
        assertNull(result.getDescription()); // Default invalid result
        
        // Verify no interaction with API key management service
        verify(apiKeyManagementService, never()).validateAndTrackUsage(anyString());
    }

    @Test
    void validate_ShouldReturnInvalid_ForEmptyApiKey() {
        // Arrange
        String apiKey = "   ";

        // Act
        ApiKeyValidationResult result = validator.validate(apiKey);

        // Assert
        assertFalse(result.isValid());
        assertNull(result.getDescription()); // Default invalid result
        
        // Verify no interaction with API key management service
        verify(apiKeyManagementService, never()).validateAndTrackUsage(anyString());
    }

    @Test
    void validate_ShouldHandleServiceException_Gracefully() {
        // Arrange
        String apiKey = "sk_testapikey1234567890";
        
        when(apiKeyManagementService.validateAndTrackUsage(apiKey))
                .thenThrow(new RuntimeException("Service unavailable"));

        // Act
        ApiKeyValidationResult result = validator.validate(apiKey);

        // Assert
        assertFalse(result.isValid());
        assertEquals("Validation error", result.getDescription());
        
        // Verify audit log still called
        verify(auditService, times(1)).logApiKeyUsage(
                eq("UNKNOWN"),
                anyString(),
                eq(false),
                isNull());
    }

    @Test
    void hashKey_ShouldReturnConsistentHash() {
        // Arrange
        String apiKey = "sk_testapikey1234567890";

        // Act - use reflection to test private method
        String hash1 = (String) ReflectionTestUtils.invokeMethod(validator, "hashKey", apiKey);
        String hash2 = (String) ReflectionTestUtils.invokeMethod(validator, "hashKey", apiKey);

        // Assert
        assertNotNull(hash1);
        assertEquals(hash1, hash2);
    }

    @Test
    void maskApiKey_ShouldMaskProperly() {
        // Arrange
        String apiKey = "sk_abcdefghijklmnopqrstuvwxyz123456";

        // Act
        String masked = (String) ReflectionTestUtils.invokeMethod(validator, "maskApiKey", apiKey);

        // Assert
        assertEquals("sk_a...123456", masked);
    }

    @Test
    void maskApiKey_ShouldReturnStars_ForShortKey() {
        // Arrange
        String shortKey = "sk_short";

        // Act
        String masked = (String) ReflectionTestUtils.invokeMethod(validator, "maskApiKey", shortKey);

        // Assert
        assertEquals("***", masked);
    }

    @Test
    void validate_ShouldTrimApiKey() {
        // Arrange
        String apiKey = "  sk_validapikey1234567890  ";
        String trimmedKey = "sk_validapikey1234567890";
        String clientId = "client-123";
        
        ApiKeyValidationResult validationResult = ApiKeyValidationResult.valid(
                clientId, Arrays.asList("ROLE_USER"), "Test");
        
        when(apiKeyManagementService.validateAndTrackUsage(trimmedKey))
                .thenReturn(validationResult);

        // Act
        ApiKeyValidationResult result = validator.validate(apiKey);

        // Assert
        assertTrue(result.isValid());
        verify(apiKeyManagementService, times(1)).validateAndTrackUsage(trimmedKey);
    }

    @Test
    void validate_ShouldNotCallAudit_WhenApiKeyIsNull() {
        // Act
        validator.validate(null);

        // Assert
        verify(auditService, never()).logApiKeyUsage(
                anyString(), anyString(), anyBoolean(), any());
    }

    @Test
    void validate_ShouldNotCallAudit_WhenApiKeyIsEmpty() {
        // Act
        validator.validate("   ");

        // Assert
        verify(auditService, never()).logApiKeyUsage(
                anyString(), anyString(), anyBoolean(), any());
    }
}