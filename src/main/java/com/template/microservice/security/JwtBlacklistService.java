package com.template.microservice.security;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing JWT token blacklist.
 * In production, this should be backed by Redis or a database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtBlacklistService {

    private final MeterRegistry meterRegistry;
    
    @Value("${security.jwt.blacklist.enabled:true}")
    private boolean enabled;
    
    @Value("${security.jwt.blacklist.cleanup.interval-hours:24}")
    private int cleanupIntervalHours;
    
    private final Map<String, BlacklistedToken> blacklist = new ConcurrentHashMap<>();
    private Counter blacklistAddCounter;
    private Counter blacklistCheckCounter;
    private Counter blacklistHitCounter;
    private Counter blacklistCleanupCounter;
    
    @PostConstruct
    public void init() {
        // Initialize metrics
        blacklistAddCounter = Counter.builder("security.jwt.blacklist.added")
                .description("Number of tokens added to blacklist")
                .register(meterRegistry);
        
        blacklistCheckCounter = Counter.builder("security.jwt.blacklist.checked")
                .description("Number of token blacklist checks")
                .register(meterRegistry);
        
        blacklistHitCounter = Counter.builder("security.jwt.blacklist.hits")
                .description("Number of blacklisted token hits")
                .register(meterRegistry);
        
        blacklistCleanupCounter = Counter.builder("security.jwt.blacklist.cleaned")
                .description("Number of tokens cleaned from blacklist")
                .register(meterRegistry);
    }
    
    /**
     * Add a token to the blacklist.
     */
    public void blacklistToken(String token, String reason, LocalDateTime expiresAt) {
        if (!enabled) {
            return;
        }
        
        BlacklistedToken blacklistedToken = BlacklistedToken.builder()
                .tokenHash(hashToken(token))
                .reason(reason)
                .blacklistedAt(LocalDateTime.now())
                .expiresAt(expiresAt != null ? expiresAt : LocalDateTime.now().plusDays(7))
                .build();
        
        blacklist.put(blacklistedToken.getTokenHash(), blacklistedToken);
        blacklistAddCounter.increment();
        
        log.info("Token blacklisted, reason: {}, expires: {}", reason, blacklistedToken.getExpiresAt());
    }
    
    /**
     * Check if a token is blacklisted.
     */
    public boolean isTokenBlacklisted(String token) {
        if (!enabled) {
            return false;
        }
        
        blacklistCheckCounter.increment();
        String tokenHash = hashToken(token);
        BlacklistedToken blacklistedToken = blacklist.get(tokenHash);
        
        if (blacklistedToken == null) {
            return false;
        }
        
        // Check if token has expired from blacklist
        if (blacklistedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            blacklist.remove(tokenHash);
            return false;
        }
        
        blacklistHitCounter.increment();
        return true;
    }
    
    /**
     * Remove a token from the blacklist (e.g., for token refresh scenarios).
     */
    public boolean removeFromBlacklist(String token) {
        if (!enabled) {
            return false;
        }
        
        String tokenHash = hashToken(token);
        return blacklist.remove(tokenHash) != null;
    }
    
    /**
     * Clean up expired blacklist entries.
     */
    @Scheduled(fixedDelayString = "${security.jwt.blacklist.cleanup.interval-hours:24}", 
               timeUnit = TimeUnit.HOURS)
    public void cleanupExpiredTokens() {
        if (!enabled) {
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        int initialSize = blacklist.size();
        
        blacklist.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().getExpiresAt().isBefore(now);
            if (expired) {
                log.debug("Removing expired blacklisted token: {}", entry.getValue().getTokenHash());
            }
            return expired;
        });
        
        int removedCount = initialSize - blacklist.size();
        if (removedCount > 0) {
            blacklistCleanupCounter.increment(removedCount);
            log.info("Cleaned up {} expired blacklisted tokens", removedCount);
        }
    }
    
    /**
     * Get blacklist statistics.
     */
    public BlacklistStats getStats() {
        int total = blacklist.size();
        long expiredCount = blacklist.values().stream()
                .filter(token -> token.getExpiresAt().isBefore(LocalDateTime.now()))
                .count();
        
        return BlacklistStats.builder()
                .totalTokens(total)
                .expiredTokens((int) expiredCount)
                .activeTokens(total - (int) expiredCount)
                .build();
    }
    
    private String hashToken(String token) {
        // In production, use a proper cryptographic hash like SHA-256
        // This is a simplified example
        return Integer.toHexString(token.hashCode());
    }
    
    /**
     * Blacklisted token information.
     */
    @lombok.Builder
    @lombok.Data
    public static class BlacklistedToken {
        private String tokenHash;
        private String reason;
        private LocalDateTime blacklistedAt;
        private LocalDateTime expiresAt;
    }
    
    /**
     * Blacklist statistics.
     */
    @lombok.Builder
    @lombok.Data
    public static class BlacklistStats {
        private int totalTokens;
        private int activeTokens;
        private int expiredTokens;
    }
}