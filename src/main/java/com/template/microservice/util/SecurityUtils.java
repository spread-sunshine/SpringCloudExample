package com.template.microservice.util;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Security utility class for cryptographic operations.
 */
@Slf4j
public class SecurityUtils {

    private static final String SHA_256 = "SHA-256";
    private static final String SHA_256_ERROR = "SHA-256 algorithm not available";

    private SecurityUtils() {
        // Utility class, prevent instantiation
    }

    /**
     * Compute SHA-256 hash of the input string.
     *
     * @param input the input string
     * @return hexadecimal representation of the SHA-256 hash
     */
    public static String computeSha256Hash(String input) {
        if (input == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error(SHA_256_ERROR, e);
            // Fallback to simpler hash, but this should not happen as SHA-256 is standard
            throw new IllegalStateException(SHA_256_ERROR, e);
        }
    }

    /**
     * Compute SHA-256 hash and encode as Base64.
     *
     * @param input the input string
     * @return Base64 encoded SHA-256 hash
     */
    public static String computeSha256HashBase64(String input) {
        if (input == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error(SHA_256_ERROR, e);
            throw new IllegalStateException(SHA_256_ERROR, e);
        }
    }

    /**
     * Convert byte array to hexadecimal string.
     *
     * @param bytes the byte array
     * @return hexadecimal string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}