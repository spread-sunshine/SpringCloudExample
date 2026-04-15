package com.template.microservice.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for auditing security events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private static final String AUDIT_LOG_PREFIX = "SECURITY_AUDIT";

    /**
     * Log authentication success event.
     */
    public void logAuthenticationSuccess(String username, String authenticationMethod, HttpServletRequest request) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event", "AUTHENTICATION_SUCCESS");
        auditData.put("username", username);
        auditData.put("authenticationMethod", authenticationMethod);
        auditData.put("timestamp", LocalDateTime.now());
        auditData.put("ipAddress", getClientIp(request));
        auditData.put("userAgent", request.getHeader("User-Agent"));
        auditData.put("endpoint", request.getRequestURI());

        logAuditEvent(auditData);
    }

    /**
     * Log authentication failure event.
     */
    public void logAuthenticationFailure(String username, String authenticationMethod, 
                                         String failureReason, HttpServletRequest request) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event", "AUTHENTICATION_FAILURE");
        auditData.put("username", username != null ? username : "UNKNOWN");
        auditData.put("authenticationMethod", authenticationMethod);
        auditData.put("failureReason", failureReason);
        auditData.put("timestamp", LocalDateTime.now());
        auditData.put("ipAddress", getClientIp(request));
        auditData.put("userAgent", request.getHeader("User-Agent"));
        auditData.put("endpoint", request.getRequestURI());

        logAuditEvent(auditData);
    }

    /**
     * Log authorization failure event.
     */
    public void logAuthorizationFailure(String username, String resource, String action, 
                                        HttpServletRequest request) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event", "AUTHORIZATION_FAILURE");
        auditData.put("username", username != null ? username : "ANONYMOUS");
        auditData.put("resource", resource);
        auditData.put("action", action);
        auditData.put("timestamp", LocalDateTime.now());
        auditData.put("ipAddress", getClientIp(request));
        auditData.put("userAgent", request.getHeader("User-Agent"));
        auditData.put("endpoint", request.getRequestURI());

        logAuditEvent(auditData);
    }

    /**
     * Log API key usage event.
     */
    public void logApiKeyUsage(String clientId, String apiKeyHash, boolean success, 
                               HttpServletRequest request) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event", success ? "API_KEY_SUCCESS" : "API_KEY_FAILURE");
        auditData.put("clientId", clientId);
        auditData.put("apiKeyHash", apiKeyHash); // Should be hash, not plain key
        auditData.put("timestamp", LocalDateTime.now());
        auditData.put("ipAddress", getClientIp(request));
        auditData.put("userAgent", request.getHeader("User-Agent"));
        auditData.put("endpoint", request.getRequestURI());

        logAuditEvent(auditData);
    }

    /**
     * Log sensitive operation event.
     */
    public void logSensitiveOperation(String operation, String target, 
                                      Map<String, Object> additionalData) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "SYSTEM";

        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event", "SENSITIVE_OPERATION");
        auditData.put("operation", operation);
        auditData.put("target", target);
        auditData.put("username", username);
        auditData.put("timestamp", LocalDateTime.now());
        
        if (additionalData != null) {
            auditData.putAll(additionalData);
        }

        logAuditEvent(auditData);
    }

    /**
     * Log password change event.
     */
    public void logPasswordChange(String username, boolean success, HttpServletRequest request) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event", success ? "PASSWORD_CHANGE_SUCCESS" : "PASSWORD_CHANGE_FAILURE");
        auditData.put("username", username);
        auditData.put("timestamp", LocalDateTime.now());
        auditData.put("ipAddress", getClientIp(request));
        auditData.put("userAgent", request.getHeader("User-Agent"));

        logAuditEvent(auditData);
    }

    /**
     * Log role change event.
     */
    public void logRoleChange(String targetUser, String oldRoles, String newRoles, 
                              String changedBy) {
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("event", "ROLE_CHANGE");
        auditData.put("targetUser", targetUser);
        auditData.put("oldRoles", oldRoles);
        auditData.put("newRoles", newRoles);
        auditData.put("changedBy", changedBy);
        auditData.put("timestamp", LocalDateTime.now());

        logAuditEvent(auditData);
    }

    private void logAuditEvent(Map<String, Object> auditData) {
        // Log as structured JSON for easier parsing
        StringBuilder logMessage = new StringBuilder();
        logMessage.append(AUDIT_LOG_PREFIX).append(": ");
        
        auditData.forEach((key, value) -> {
            logMessage.append(key).append("=");
            if (value instanceof String) {
                logMessage.append("\"").append(value).append("\"");
            } else {
                logMessage.append(value);
            }
            logMessage.append(" ");
        });

        log.info(logMessage.toString().trim());
        
        // In production, this should also send to a dedicated audit log system
        // or write to an audit database table
    }

    String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Get first IP in the chain
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Audit event types.
     */
    public enum AuditEvent {
        AUTHENTICATION_SUCCESS,
        AUTHENTICATION_FAILURE,
        AUTHORIZATION_FAILURE,
        API_KEY_SUCCESS,
        API_KEY_FAILURE,
        SENSITIVE_OPERATION,
        PASSWORD_CHANGE,
        ROLE_CHANGE
    }
}