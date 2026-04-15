package com.template.microservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private SecretKey testKey;
    private final String testSecret = "test-secret-key-for-jwt-token-generation-and-validation-at-least-256-bits-long";
    private final long validityMs = 3600000L; // 1 hour
    private final long refreshValidityMs = 604800000L; // 7 days

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        testKey = Keys.hmacShaKeyFor(testSecret.getBytes());
        
        // Set private fields using reflection
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", testSecret);
        ReflectionTestUtils.setField(jwtTokenProvider, "validityInMilliseconds", validityMs);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshValidityInMilliseconds", refreshValidityMs);
        ReflectionTestUtils.setField(jwtTokenProvider, "key", testKey);
    }

    @Test
    void createToken_ShouldGenerateValidJwtToken() {
        // Arrange
        String username = "testuser";
        Collection<SimpleGrantedAuthority> authorities = Arrays.asList(
            new SimpleGrantedAuthority("ROLE_USER"),
            new SimpleGrantedAuthority("ROLE_ADMIN")
        );

        // Act
        String token = jwtTokenProvider.createToken(username, authorities);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        
        // Verify token can be parsed
        Claims claims = Jwts.parser()
                .verifyWith(testKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        assertEquals(username, claims.getSubject());
        assertTrue(claims.get("auth").toString().contains("ROLE_USER"));
        assertTrue(claims.get("auth").toString().contains("ROLE_ADMIN"));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        
        // Verify expiration is in the future
        assertTrue(claims.getExpiration().after(new Date()));
    }

    @Test
    void createToken_ShouldHandleEmptyAuthorities() {
        // Arrange
        String username = "testuser";
        Collection<SimpleGrantedAuthority> authorities = List.of();

        // Act
        String token = jwtTokenProvider.createToken(username, authorities);

        // Assert
        assertNotNull(token);
        
        Claims claims = Jwts.parser()
                .verifyWith(testKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        assertEquals(username, claims.getSubject());
        assertNotNull(claims.get("auth"));
    }

    @Test
    void createRefreshToken_ShouldGenerateValidRefreshToken() {
        // Arrange
        String username = "testuser";

        // Act
        String refreshToken = jwtTokenProvider.createRefreshToken(username);

        // Assert
        assertNotNull(refreshToken);
        assertFalse(refreshToken.isEmpty());
        
        Claims claims = Jwts.parser()
                .verifyWith(testKey)
                .build()
                .parseSignedClaims(refreshToken)
                .getPayload();
        
        assertEquals(username, claims.getSubject());
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertTrue(claims.getExpiration().after(new Date()));
    }

    @Test
    void createRefreshToken_ShouldHaveLongerExpirationThanAccessToken() {
        // Arrange
        String username = "testuser";
        Collection<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        // Act
        String accessToken = jwtTokenProvider.createToken(username, authorities);
        String refreshToken = jwtTokenProvider.createRefreshToken(username);

        // Assert
        Claims accessClaims = Jwts.parser()
                .verifyWith(testKey)
                .build()
                .parseSignedClaims(accessToken)
                .getPayload();
        
        Claims refreshClaims = Jwts.parser()
                .verifyWith(testKey)
                .build()
                .parseSignedClaims(refreshToken)
                .getPayload();
        
        long accessExpiration = accessClaims.getExpiration().getTime();
        long refreshExpiration = refreshClaims.getExpiration().getTime();
        long issuedAt = accessClaims.getIssuedAt().getTime();
        
        long accessDuration = accessExpiration - issuedAt;
        long refreshDuration = refreshExpiration - issuedAt;
        
        assertTrue(refreshDuration > accessDuration, 
                "Refresh token should have longer expiration than access token");
    }

    @Test
    void getAuthentication_ShouldReturnValidAuthentication() {
        // Arrange
        String username = "testuser";
        Collection<SimpleGrantedAuthority> authorities = Arrays.asList(
            new SimpleGrantedAuthority("ROLE_USER"),
            new SimpleGrantedAuthority("ROLE_ADMIN")
        );
        
        String token = jwtTokenProvider.createToken(username, authorities);

        // Act
        var authentication = jwtTokenProvider.getAuthentication(token);

        // Assert
        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
        
        User principal = (User) authentication.getPrincipal();
        assertEquals(username, principal.getUsername());
        
        var authAuthorities = authentication.getAuthorities();
        assertEquals(2, authAuthorities.size());
        assertTrue(authAuthorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_USER")));
        assertTrue(authAuthorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void validateToken_ShouldReturnTrueForValidToken() {
        // Arrange
        String username = "testuser";
        Collection<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        String token = jwtTokenProvider.createToken(username, authorities);

        // Act
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void validateToken_ShouldReturnFalseForExpiredToken() {
        // Arrange
        String username = "testuser";
        Collection<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        
        // Create a token with immediate expiration
        ReflectionTestUtils.setField(jwtTokenProvider, "validityInMilliseconds", -1000L);
        String token = jwtTokenProvider.createToken(username, authorities);
        
        // Reset validity
        ReflectionTestUtils.setField(jwtTokenProvider, "validityInMilliseconds", validityMs);

        // Act
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void validateToken_ShouldReturnFalseForInvalidToken() {
        // Arrange
        String invalidToken = "invalid.token.string";

        // Act
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void validateToken_ShouldReturnFalseForMalformedToken() {
        // Arrange
        String malformedToken = "header.payload.signature";

        // Act
        boolean isValid = jwtTokenProvider.validateToken(malformedToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void validateToken_ShouldReturnFalseForTokenSignedWithDifferentKey() {
        // Arrange
        String username = "testuser";
        Collection<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        
        // Create token with different secret
        JwtTokenProvider otherProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(otherProvider, "secretKey", "different-secret-key-longer-than-256-bits-for-testing-purposes");
        ReflectionTestUtils.setField(otherProvider, "validityInMilliseconds", validityMs);
        ReflectionTestUtils.invokeMethod(otherProvider, "init");
        
        String token = otherProvider.createToken(username, authorities);

        // Act
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void getUsernameFromToken_ShouldReturnUsernameForValidToken() {
        // Arrange
        String username = "testuser";
        Collection<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        String token = jwtTokenProvider.createToken(username, authorities);

        // Act
        String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);

        // Assert
        assertEquals(username, extractedUsername);
    }

    @Test
    void getUsernameFromToken_ShouldThrowExceptionForInvalidToken() {
        // Arrange
        String invalidToken = "invalid.token.string";

        // Act & Assert
        assertThrows(Exception.class, () -> {
            jwtTokenProvider.getUsernameFromToken(invalidToken);
        });
    }

    @Test
    void init_ShouldInitializeSecretKey() {
        // Arrange
        JwtTokenProvider newProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(newProvider, "secretKey", testSecret);

        // Act
        ReflectionTestUtils.invokeMethod(newProvider, "init");

        // Assert - Key should be initialized
        SecretKey key = (SecretKey) ReflectionTestUtils.getField(newProvider, "key");
        assertNotNull(key);
        
        // Verify key can be used to sign tokens
        String token = newProvider.createToken("test", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        assertNotNull(token);
        assertTrue(newProvider.validateToken(token));
    }

    @Test
    void tokenExpiration_ShouldRespectConfiguredValidity() {
        // Arrange
        long customValidity = 5000L; // 5 seconds
        ReflectionTestUtils.setField(jwtTokenProvider, "validityInMilliseconds", customValidity);
        
        String username = "testuser";
        Collection<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        // Act
        String token = jwtTokenProvider.createToken(username, authorities);

        // Assert
        Claims claims = Jwts.parser()
                .verifyWith(testKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        long expectedExpiration = claims.getIssuedAt().getTime() + customValidity;
        long actualExpiration = claims.getExpiration().getTime();
        
        // Allow small difference due to clock resolution
        assertTrue(Math.abs(expectedExpiration - actualExpiration) < 1000,
                "Token expiration should match configured validity");
    }

    @Test
    void getAuthentication_ShouldHandleNullToken() {
        // Arrange
        String nullToken = null;

        // Act & Assert
        assertThrows(Exception.class, () -> {
            jwtTokenProvider.getAuthentication(nullToken);
        });
    }

    @Test
    void getAuthentication_ShouldHandleEmptyToken() {
        // Arrange
        String emptyToken = "";

        // Act & Assert
        assertThrows(Exception.class, () -> {
            jwtTokenProvider.getAuthentication(emptyToken);
        });
    }
}