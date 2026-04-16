package com.template.microservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // Common errors
    SUCCESS("00000", "Success", HttpStatus.OK),
    INTERNAL_SERVER_ERROR("50000", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE("50001", "Service unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    
    // Validation errors (400xx)
    VALIDATION_ERROR("40000", "Validation failed", HttpStatus.BAD_REQUEST),
    INVALID_PARAMETER("40001", "Invalid parameter", HttpStatus.BAD_REQUEST),
    MISSING_PARAMETER("40002", "Missing required parameter", HttpStatus.BAD_REQUEST),
    INVALID_FORMAT("40003", "Invalid data format", HttpStatus.BAD_REQUEST),
    CONSTRAINT_VIOLATION("40004", "Constraint violation", HttpStatus.BAD_REQUEST),
    
    // Authentication errors (401xx)
    AUTHENTICATION_FAILED("40100", "Authentication failed", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS("40101", "Invalid credentials", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("40102", "Token expired", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("40103", "Invalid token", HttpStatus.UNAUTHORIZED),
    TOKEN_REVOKED("40104", "Token revoked", HttpStatus.UNAUTHORIZED),
    
    // Authorization errors (403xx)
    ACCESS_DENIED("40300", "Access denied", HttpStatus.FORBIDDEN),
    INSUFFICIENT_PERMISSIONS("40301", "Insufficient permissions", HttpStatus.FORBIDDEN),
    RESOURCE_FORBIDDEN("40302", "Resource forbidden", HttpStatus.FORBIDDEN),
    
    // Resource errors (404xx)
    RESOURCE_NOT_FOUND("40400", "Resource not found", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND("40401", "User not found", HttpStatus.NOT_FOUND),
    ENTITY_NOT_FOUND("40402", "Entity not found", HttpStatus.NOT_FOUND),
    
    // Business logic errors (409xx)
    CONFLICT("40900", "Resource conflict", HttpStatus.CONFLICT),
    DUPLICATE_RESOURCE("40901", "Duplicate resource", HttpStatus.CONFLICT),
    RESOURCE_IN_USE("40902", "Resource in use", HttpStatus.CONFLICT),
    
    // Rate limiting errors (429xx)
    RATE_LIMIT_EXCEEDED("42900", "Rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS),
    
    // External service errors (502xx)
    EXTERNAL_SERVICE_ERROR("50200", "External service error", HttpStatus.BAD_GATEWAY),
    SERVICE_TIMEOUT("50201", "Service timeout", HttpStatus.GATEWAY_TIMEOUT),
    
    // Database errors (503xx)
    DATABASE_ERROR("50300", "Database error", HttpStatus.SERVICE_UNAVAILABLE),
    DATABASE_CONNECTION_FAILED("50301", "Database connection failed", HttpStatus.SERVICE_UNAVAILABLE),
    
    // Cache errors (504xx)
    CACHE_ERROR("50400", "Cache error", HttpStatus.SERVICE_UNAVAILABLE),
    
    // Message queue errors (505xx)
    MESSAGE_QUEUE_ERROR("50500", "Message queue error", HttpStatus.SERVICE_UNAVAILABLE),
    
    // File upload errors (406xx)
    FILE_UPLOAD_ERROR("40600", "File upload error", HttpStatus.NOT_ACCEPTABLE),
    FILE_TOO_LARGE("40601", "File too large", HttpStatus.NOT_ACCEPTABLE),
    INVALID_FILE_TYPE("40602", "Invalid file type", HttpStatus.NOT_ACCEPTABLE);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public static ErrorCode fromCode(String code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.getCode().equals(code)) {
                return errorCode;
            }
        }
        return INTERNAL_SERVER_ERROR;
    }

    public BusinessException toException() {
        return new BusinessException(this.message, this.code, this.httpStatus);
    }

    public BusinessException toException(String detail) {
        return new BusinessException(this.message + ": " + detail, this.code, this.httpStatus);
    }

    public BusinessException toException(Throwable cause) {
        return new BusinessException(this.message, cause, this.code, this.httpStatus);
    }
}