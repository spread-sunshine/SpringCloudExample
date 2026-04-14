package com.example.microservice.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;
    
    @Value("${rate.limit.default:100}")
    private int defaultLimit;
    
    @Value("${rate.limit.window.seconds:60}")
    private int windowSeconds;
    
    @Value("${rate.limit.enabled:true}")
    private boolean enabled;

    public RateLimitService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isRateLimited(String clientId, String endpoint) {
        if (!enabled) {
            return false;
        }
        
        String key = buildKey(clientId, endpoint);
        Long currentCount = redisTemplate.opsForValue().increment(key);
        
        if (currentCount == 1) {
            // First request in window, set expiration
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }
        
        int limit = getLimitForEndpoint(endpoint);
        return currentCount != null && currentCount > limit;
    }

    public RateLimitInfo getRateLimitInfo(String clientId, String endpoint) {
        String key = buildKey(clientId, endpoint);
        Long currentCount = redisTemplate.opsForValue().get(key) != null ? 
                Long.parseLong(redisTemplate.opsForValue().get(key)) : 0L;
        
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (ttl == null || ttl < 0) {
            ttl = (long) windowSeconds;
        }
        
        int limit = getLimitForEndpoint(endpoint);
        long remaining = Math.max(0, limit - currentCount);
        long resetTime = Instant.now().plusSeconds(ttl).getEpochSecond();
        
        return new RateLimitInfo(limit, remaining, resetTime, windowSeconds);
    }

    public void resetRateLimit(String clientId, String endpoint) {
        String key = buildKey(clientId, endpoint);
        redisTemplate.delete(key);
    }

    private String buildKey(String clientId, String endpoint) {
        return String.format("ratelimit:%s:%s", endpoint, clientId);
    }

    private int getLimitForEndpoint(String endpoint) {
        // Could be enhanced to have different limits for different endpoints
        if (endpoint.contains("/api/admin/")) {
            return 50; // Stricter limit for admin endpoints
        } else if (endpoint.contains("/api/user/")) {
            return 200; // Higher limit for user endpoints
        }
        return defaultLimit;
    }

    @Data
    public static class RateLimitInfo {
        private final int limit;
        private final long remaining;
        private final long resetTime; // Unix timestamp
        private final long windowSeconds;
        
        public Duration getTimeUntilReset() {
            long now = Instant.now().getEpochSecond();
            return Duration.ofSeconds(resetTime - now);
        }
        
        public boolean isExceeded() {
            return remaining <= 0;
        }
    }
}