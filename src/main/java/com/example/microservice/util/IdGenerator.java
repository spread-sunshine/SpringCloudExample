package com.example.microservice.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Component
@Slf4j
public class IdGenerator {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

    /**
     * Generate a UUID-based ID
     */
    public String generateUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate a time-based ID (timestamp + random)
     */
    public String generateTimeBasedId() {
        long timestamp = Instant.now().toEpochMilli();
        int random = secureRandom.nextInt(10000);
        return String.format("%d-%04d", timestamp, random);
    }

    /**
     * Generate a secure random token
     */
    public String generateSecureToken(int byteLength) {
        byte[] randomBytes = new byte[byteLength];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }

    /**
     * Generate a short ID (8 characters)
     */
    public String generateShortId() {
        byte[] randomBytes = new byte[6];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }

    /**
     * Generate a numeric ID with prefix
     */
    public String generateNumericId(String prefix, int length) {
        StringBuilder sb = new StringBuilder();
        if (prefix != null) {
            sb.append(prefix).append("-");
        }
        
        for (int i = 0; i < length; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        
        return sb.toString();
    }

    /**
     * Generate a snowflake-like ID (timestamp + machine id + sequence)
     */
    public synchronized long generateSnowflakeId(long machineId) {
        long timestamp = System.currentTimeMillis() - 1609459200000L; // Custom epoch
        long sequence = secureRandom.nextInt(4096);
        
        return (timestamp << 22) | (machineId << 12) | sequence;
    }
}