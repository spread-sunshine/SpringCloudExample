package com.template.microservice.security;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtBlacklistServiceTest {

    private JwtBlacklistService jwtBlacklistService;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        jwtBlacklistService = new JwtBlacklistService(meterRegistry);
        ReflectionTestUtils.setField(jwtBlacklistService, "enabled", true);
        ReflectionTestUtils.setField(jwtBlacklistService, "cleanupIntervalHours", 24);
        
        // Initialize the service (simulate @PostConstruct)
        ReflectionTestUtils.invokeMethod(jwtBlacklistService, "init");
    }

    @Test
    void blacklistToken_ShouldAddToken_WhenEnabled() {
        // Arrange
        String token = "test.jwt.token";
        String reason = "logout";
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);

        // Act
        jwtBlacklistService.blacklistToken(token, reason, expiresAt);

        // Assert
        assertTrue(jwtBlacklistService.isTokenBlacklisted(token));
        
        // Check metrics
        Counter counter = meterRegistry.find("security.jwt.blacklist.added").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    @Test
    void blacklistToken_ShouldNotAddToken_WhenDisabled() {
        // Arrange
        ReflectionTestUtils.setField(jwtBlacklistService, "enabled", false);
        String token = "test.jwt.token";
        String reason = "logout";
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(1);

        // Act
        jwtBlacklistService.blacklistToken(token, reason, expiresAt);

        // Assert
        assertFalse(jwtBlacklistService.isTokenBlacklisted(token));
    }

    @Test
    void isTokenBlacklisted_ShouldReturnFalse_ForNonBlacklistedToken() {
        // Arrange
        String token = "non.existent.token";

        // Act & Assert
        assertFalse(jwtBlacklistService.isTokenBlacklisted(token));
    }

    @Test
    void isTokenBlacklisted_ShouldReturnTrue_ForBlacklistedToken() {
        // Arrange
        String token = "test.jwt.token";
        jwtBlacklistService.blacklistToken(token, "logout", LocalDateTime.now().plusDays(1));

        // Act & Assert
        assertTrue(jwtBlacklistService.isTokenBlacklisted(token));
        
        // Check metrics
        Counter checkCounter = meterRegistry.find("security.jwt.blacklist.checked").counter();
        Counter hitCounter = meterRegistry.find("security.jwt.blacklist.hits").counter();
        assertNotNull(checkCounter);
        assertNotNull(hitCounter);
        assertEquals(1.0, checkCounter.count());
        assertEquals(1.0, hitCounter.count());
    }

    @Test
    void isTokenBlacklisted_ShouldReturnFalse_ForExpiredBlacklistedToken() {
        // Arrange
        String token = "expired.token";
        jwtBlacklistService.blacklistToken(token, "logout", LocalDateTime.now().minusDays(1));

        // Act & Assert
        assertFalse(jwtBlacklistService.isTokenBlacklisted(token));
    }

    @Test
    void removeFromBlacklist_ShouldRemoveToken() {
        // Arrange
        String token = "test.jwt.token";
        jwtBlacklistService.blacklistToken(token, "logout", LocalDateTime.now().plusDays(1));
        assertTrue(jwtBlacklistService.isTokenBlacklisted(token));

        // Act
        boolean removed = jwtBlacklistService.removeFromBlacklist(token);

        // Assert
        assertTrue(removed);
        assertFalse(jwtBlacklistService.isTokenBlacklisted(token));
    }

    @Test
    void removeFromBlacklist_ShouldReturnFalse_ForNonExistentToken() {
        // Arrange
        String token = "non.existent.token";

        // Act
        boolean removed = jwtBlacklistService.removeFromBlacklist(token);

        // Assert
        assertFalse(removed);
    }

    @Test
    void cleanupExpiredTokens_ShouldRemoveExpiredTokens() {
        // Arrange
        String expiredToken = "expired.token";
        String validToken = "valid.token";
        
        jwtBlacklistService.blacklistToken(expiredToken, "logout", LocalDateTime.now().minusDays(1));
        jwtBlacklistService.blacklistToken(validToken, "logout", LocalDateTime.now().plusDays(1));

        // Act
        ReflectionTestUtils.invokeMethod(jwtBlacklistService, "cleanupExpiredTokens");

        // Assert
        assertFalse(jwtBlacklistService.isTokenBlacklisted(expiredToken));
        assertTrue(jwtBlacklistService.isTokenBlacklisted(validToken));
        
        // Check cleanup metrics
        Counter cleanupCounter = meterRegistry.find("security.jwt.blacklist.cleaned").counter();
        assertNotNull(cleanupCounter);
        assertEquals(1.0, cleanupCounter.count());
    }

    @Test
    void getStats_ShouldReturnCorrectStatistics() {
        // Arrange
        String token1 = "token1";
        String token2 = "token2";
        String token3 = "token3"; // Will be expired
        
        jwtBlacklistService.blacklistToken(token1, "logout", LocalDateTime.now().plusDays(1));
        jwtBlacklistService.blacklistToken(token2, "logout", LocalDateTime.now().plusDays(2));
        jwtBlacklistService.blacklistToken(token3, "logout", LocalDateTime.now().minusDays(1));

        // Act
        JwtBlacklistService.BlacklistStats stats = jwtBlacklistService.getStats();

        // Assert
        assertEquals(3, stats.getTotalTokens());
        assertEquals(1, stats.getExpiredTokens());
        assertEquals(2, stats.getActiveTokens());
    }

    @Test
    void hashToken_ShouldReturnConsistentHash() {
        // Arrange
        String token = "test.jwt.token";

        // Act - use reflection to test private method
        String hash1 = (String) ReflectionTestUtils.invokeMethod(jwtBlacklistService, "hashToken", token);
        String hash2 = (String) ReflectionTestUtils.invokeMethod(jwtBlacklistService, "hashToken", token);

        // Assert
        assertNotNull(hash1);
        assertEquals(hash1, hash2);
    }

    @Test
    void blacklistedTokenBuilder_ShouldCreateValidObject() {
        // Arrange & Act
        JwtBlacklistService.BlacklistedToken token = JwtBlacklistService.BlacklistedToken.builder()
                .tokenHash("hash123")
                .reason("logout")
                .blacklistedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        // Assert
        assertEquals("hash123", token.getTokenHash());
        assertEquals("logout", token.getReason());
        assertNotNull(token.getBlacklistedAt());
        assertNotNull(token.getExpiresAt());
    }
}