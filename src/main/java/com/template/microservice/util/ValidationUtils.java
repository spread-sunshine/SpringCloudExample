package com.template.microservice.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ValidationUtils {

    private final Validator validator;

    public ValidationUtils() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            this.validator = factory.getValidator();
        }
    }

    /**
     * Validate object and throw exception if validation fails
     */
    public <T> void validateAndThrow(T object) {
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new IllegalArgumentException("Validation failed: " + message);
        }
    }

    /**
     * Validate object and return validation errors
     */
    public <T> Set<String> validate(T object) {
        return validator.validate(object).stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.toSet());
    }

    /**
     * Validate object with specific validation group
     */
    public <T> Set<String> validate(T object, Class<?>... groups) {
        return validator.validate(object, groups).stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.toSet());
    }

    /**
     * Check if object is valid
     */
    public <T> boolean isValid(T object) {
        return validator.validate(object).isEmpty();
    }

    /**
     * Validate email format
     */
    public boolean isValidEmail(String email) {
        if (email == null) return false;
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    /**
     * Validate phone number format (simple)
     */
    public boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return false;
        // Simple validation - adjust for your needs
        String phoneRegex = "^[+]?[0-9]{10,15}$";
        return phoneNumber.replaceAll("[\\s-()]", "").matches(phoneRegex);
    }

    /**
     * Validate password strength
     */
    public boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) return false;
        
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");
        
        // Require at least 3 of the 4 criteria
        int criteriaMet = 0;
        if (hasUpper) criteriaMet++;
        if (hasLower) criteriaMet++;
        if (hasDigit) criteriaMet++;
        if (hasSpecial) criteriaMet++;
        
        return criteriaMet >= 3;
    }

    /**
     * Validate URL format
     */
    public boolean isValidUrl(String url) {
        if (url == null) return false;
        try {
            new java.net.URL(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validate date string format (ISO 8601)
     */
    public boolean isValidIsoDate(String date) {
        if (date == null) return false;
        String isoRegex = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:\\d{2})$";
        return date.matches(isoRegex);
    }

    /**
     * Sanitize input string
     */
    public String sanitizeInput(String input) {
        if (input == null) return null;
        // Remove potentially dangerous characters
        return input.replaceAll("[<>\"'&;]", "");
    }

    /**
     * Truncate string to max length
     */
    public String truncate(String input, int maxLength) {
        if (input == null || input.length() <= maxLength) {
            return input;
        }
        return input.substring(0, maxLength - 3) + "...";
    }
}