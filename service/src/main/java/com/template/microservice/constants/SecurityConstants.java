package com.template.microservice.constants;

/**
 * Security-related constants.
 */
public final class SecurityConstants {

    private SecurityConstants() {
        // Utility class, prevent instantiation
    }

    // JWT
    public static final String JWT_SECRET_DEFAULT = "your-secret-key-change-in-production-with-at-least-256-bits";
    public static final long JWT_EXPIRATION_DEFAULT = 86400000L; // 24 hours
    public static final long JWT_REFRESH_EXPIRATION_DEFAULT = 604800000L; // 7 days

    // API Key
    public static final int API_KEY_EXPIRATION_DAYS_DEFAULT = 90;
    public static final int API_KEY_WARNING_DAYS_DEFAULT = 7;
    public static final int API_KEY_LENGTH_DEFAULT = 32;
    public static final String API_KEY_PREFIX_DEFAULT = "sk_";

    // Headers
    public static final String API_KEY_HEADER = "X-API-Key";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    // Security Schemes
    public static final String SECURITY_SCHEME_BEARER = "bearerAuth";
    public static final String SECURITY_SCHEME_API_KEY = "apiKey";
    public static final String SECURITY_SCHEME_BASIC = "basicAuth";

    // Roles
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_API = "ROLE_API";

    // Audit Operations
    public static final String AUDIT_API_KEY_GENERATION = "API_KEY_GENERATION";
    public static final String AUDIT_API_KEY_ROTATION = "API_KEY_ROTATION";
    public static final String AUDIT_API_KEY_REVOCATION = "API_KEY_REVOCATION";

    // Validation
    public static final String INVALID_KEY_MESSAGE_INACTIVE = "Key is inactive";
    public static final String INVALID_KEY_MESSAGE_EXPIRED = "Key has expired";
    public static final String DEACTIVATION_REASON_EXPIRED = "Expired";
}