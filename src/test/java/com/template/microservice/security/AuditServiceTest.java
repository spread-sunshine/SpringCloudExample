package com.template.microservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    private AuditService auditService;
    
    @Mock
    private Logger logger;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private Authentication authentication;
    
    @Mock
    private SecurityContext securityContext;
    
    @Captor
    private ArgumentCaptor<String> logCaptor;

    @BeforeEach
    void setUp() {
        auditService = new AuditService();
        // We can't easily verify SLF4J logs, so we'll test the logic paths
        // and verify the methods don't throw exceptions
    }

    @Test
    void logAuthenticationSuccess_ShouldNotThrowException() {
        // Arrange
        when(request.getHeader("User-Agent")).thenReturn("TestAgent");
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");
        
        // Act & Assert (should not throw)
        assertDoesNotThrow(() -> 
            auditService.logAuthenticationSuccess("testuser", "JWT", request)
        );
    }

    @Test
    void logAuthenticationSuccess_ShouldHandleNullHeaders() {
        // Arrange
        when(request.getHeader("User-Agent")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // Act & Assert (should not throw)
        assertDoesNotThrow(() -> 
            auditService.logAuthenticationSuccess("testuser", "API_KEY", request)
        );
    }

    @Test
    void logAuthenticationFailure_ShouldNotThrowException() {
        // Arrange
        when(request.getHeader("User-Agent")).thenReturn("TestAgent");
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");

        // Act & Assert (should not throw)
        assertDoesNotThrow(() -> 
            auditService.logAuthenticationFailure("testuser", "JWT", "Invalid credentials", request)
        );
    }

    @Test
    void logAuthenticationFailure_ShouldHandleNullUsername() {
        // Arrange
        when(request.getHeader("User-Agent")).thenReturn("TestAgent");
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");

        // Act & Assert (should not throw)
        assertDoesNotThrow(() -> 
            auditService.logAuthenticationFailure(null, "JWT", "Invalid credentials", request)
        );
    }

    @Test
    void logAuthorizationFailure_ShouldNotThrowException() {
        // Arrange
        when(request.getHeader("User-Agent")).thenReturn("TestAgent");
        when(request.getRequestURI()).thenReturn("/api/admin/users");
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");

        // Act & Assert (should not throw)
        assertDoesNotThrow(() -> 
            auditService.logAuthorizationFailure("testuser", "/api/admin/users", "GET", request)
        );
    }

    @Test
    void logAuthorizationFailure_ShouldHandleAnonymousUser() {
        // Arrange
        when(request.getHeader("User-Agent")).thenReturn("TestAgent");
        when(request.getRequestURI()).thenReturn("/api/admin/users");
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");

        // Act & Assert (should not throw)
        assertDoesNotThrow(() -> 
            auditService.logAuthorizationFailure(null, "/api/admin/users", "GET", request)
        );
    }

    @Test
    void logApiKeyUsage_ShouldNotThrowException() {
        // Arrange
        when(request.getHeader("User-Agent")).thenReturn("TestAgent");
        when(request.getRequestURI()).thenReturn("/api/data");
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");

        // Act & Assert (should not throw)
        assertDoesNotThrow(() -> 
            auditService.logApiKeyUsage("client123", "hashedKey123", true, request)
        );
    }

    @Test
    void logApiKeyUsage_ShouldHandleNullRequest() {
        // Act & Assert (should not throw)
        assertDoesNotThrow(() -> 
            auditService.logApiKeyUsage("client123", "hashedKey123", false, null)
        );
    }

    @Test
    void logSensitiveOperation_ShouldNotThrowException() {
        // Arrange
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("admin");

        // Act & Assert (should not throw)
        assertDoesNotThrow(() -> 
            auditService.logSensitiveOperation("USER_DELETION", "user123", null)
        );
        
        // Cleanup
        SecurityContextHolder.clearContext();
    }

    @Test
    void logSensitiveOperation_ShouldHandleSystemUser_WhenNoAuthentication() {
        // Arrange
        SecurityContextHolder.clearContext(); // No authentication

        // Act & Assert (should not throw)
        assertDoesNotThrow(() -> 
            auditService.logSensitiveOperation("SYSTEM_TASK", "cleanup", null)
        );
    }

    @Test
    void logSensitiveOperation_ShouldIncludeAdditionalData() {
        // Arrange
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("admin");
        
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("targetId", "123");
        additionalData.put("reason", "violation");
        additionalData.put("timestamp", System.currentTimeMillis());

        // Act & Assert (should not throw)
        assertDoesNotThrow(() -> 
            auditService.logSensitiveOperation("ROLE_CHANGE", "user456", additionalData)
        );
        
        // Cleanup
        SecurityContextHolder.clearContext();
    }

    @Test
    void logPasswordChange_ShouldNotThrowException() {
        // Arrange
        when(request.getHeader("User-Agent")).thenReturn("TestAgent");
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");

        // Act & Assert (should not throw)
        assertDoesNotThrow(() -> 
            auditService.logPasswordChange("testuser", true, request)
        );
    }

    @Test
    void logRoleChange_ShouldNotThrowException() {
        // Act & Assert (should not throw)
        assertDoesNotThrow(() -> 
            auditService.logRoleChange("targetuser", "ROLE_USER", "ROLE_ADMIN,ROLE_USER", "admin")
        );
    }

    @Test
    void getClientIp_ShouldReturnXForwardedFor_WhenPresent() {
        // Arrange
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");
        when(request.getHeader("X-Real-IP")).thenReturn("10.0.0.1");
        when(request.getRemoteAddr()).thenReturn("172.16.0.1");

        // Act
        String ip = auditService.getClientIp(request);

        // Assert
        assertEquals("192.168.1.1", ip);
    }

    @Test
    void getClientIp_ShouldReturnXRealIp_WhenXForwardedForMissing() {
        // Arrange
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("10.0.0.1");
        when(request.getRemoteAddr()).thenReturn("172.16.0.1");

        // Act
        String ip = auditService.getClientIp(request);

        // Assert
        assertEquals("10.0.0.1", ip);
    }

    @Test
    void getClientIp_ShouldReturnRemoteAddr_WhenNoHeaders() {
        // Arrange
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("172.16.0.1");

        // Act
        String ip = auditService.getClientIp(request);

        // Assert
        assertEquals("172.16.0.1", ip);
    }

    @Test
    void auditEventEnum_ShouldHaveAllValues() {
        // Act
        AuditService.AuditEvent[] events = AuditService.AuditEvent.values();

        // Assert
        assertEquals(8, events.length);
        assertArrayEquals(new AuditService.AuditEvent[] {
            AuditService.AuditEvent.AUTHENTICATION_SUCCESS,
            AuditService.AuditEvent.AUTHENTICATION_FAILURE,
            AuditService.AuditEvent.AUTHORIZATION_FAILURE,
            AuditService.AuditEvent.API_KEY_SUCCESS,
            AuditService.AuditEvent.API_KEY_FAILURE,
            AuditService.AuditEvent.SENSITIVE_OPERATION,
            AuditService.AuditEvent.PASSWORD_CHANGE,
            AuditService.AuditEvent.ROLE_CHANGE
        }, events);
    }
}