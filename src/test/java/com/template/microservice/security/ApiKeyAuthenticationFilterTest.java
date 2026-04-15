package com.template.microservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthenticationFilterTest {

    private ApiKeyAuthenticationFilter filter;
    
    @Mock
    private ApiKeyValidator apiKeyValidator;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private FilterChain filterChain;
    
    @Mock
    private SecurityContext securityContext;
    
    @Captor
    private ArgumentCaptor<Authentication> authenticationCaptor;

    @BeforeEach
    void setUp() {
        filter = new ApiKeyAuthenticationFilter(apiKeyValidator);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void doFilterInternal_ShouldSetAuthentication_WhenValidApiKeyHeaderPresent() throws ServletException, IOException {
        // Arrange
        String apiKey = "sk_validapikey1234567890abcdef";
        String clientId = "client-123";
        var authorities = Arrays.asList("ROLE_USER");
        
        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        
        ApiKeyValidationResult validationResult = ApiKeyValidationResult.valid(
                clientId, authorities, "Test client");
        when(apiKeyValidator.validate(apiKey)).thenReturn(validationResult);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(securityContext, times(1)).setAuthentication(authenticationCaptor.capture());
        
        Authentication auth = authenticationCaptor.getValue();
        assertNotNull(auth);
        assertEquals(clientId, auth.getName());
        assertEquals(1, auth.getAuthorities().size());
        assertEquals("ROLE_USER", auth.getAuthorities().iterator().next().getAuthority());
        assertTrue(auth.isAuthenticated());
        
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldSetAuthentication_WhenValidAuthorizationHeaderPresent() throws ServletException, IOException {
        // Arrange
        String apiKey = "sk_validapikey1234567890abcdef";
        String clientId = "client-123";
        var authorities = Arrays.asList("ROLE_USER");
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + apiKey);
        
        ApiKeyValidationResult validationResult = ApiKeyValidationResult.valid(
                clientId, authorities, "Test client");
        when(apiKeyValidator.validate(apiKey)).thenReturn(validationResult);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(securityContext, times(1)).setAuthentication(authenticationCaptor.capture());
        
        Authentication auth = authenticationCaptor.getValue();
        assertNotNull(auth);
        assertEquals(clientId, auth.getName());
        assertEquals(1, auth.getAuthorities().size());
        
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldNotSetAuthentication_WhenNoApiKeyHeaderPresent() throws ServletException, IOException {
        // Arrange
        when(request.getHeader("X-API-Key")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(null);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldNotSetAuthentication_WhenInvalidApiKey() throws ServletException, IOException {
        // Arrange
        String apiKey = "sk_invalidapikey";
        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        
        when(apiKeyValidator.validate(apiKey)).thenReturn(ApiKeyValidationResult.invalid("Invalid key"));

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldNotSetAuthentication_WhenApiKeyValidatorThrowsException() throws ServletException, IOException {
        // Arrange
        String apiKey = "sk_testapikey";
        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        
        when(apiKeyValidator.validate(apiKey)).thenThrow(new RuntimeException("Validation error"));

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(securityContext, never()).setAuthentication(any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldHandleMalformedAuthorizationHeader() throws ServletException, IOException {
        // Arrange
        String malformedHeader = "InvalidBearerFormat";
        when(request.getHeader("Authorization")).thenReturn(malformedHeader);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(securityContext, never()).setAuthentication(any());
        verify(apiKeyValidator, never()).validate(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldHandleEmptyApiKey() throws ServletException, IOException {
        // Arrange
        String emptyApiKey = "";
        when(request.getHeader("X-API-Key")).thenReturn(emptyApiKey);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(securityContext, never()).setAuthentication(any());
        verify(apiKeyValidator, never()).validate(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void extractApiKey_ShouldExtractFromXApiKeyHeader() {
        // Arrange
        String apiKey = "sk_testapikey";
        when(request.getHeader("X-API-Key")).thenReturn(apiKey);

        // Act
        String result = filter.extractApiKey(request);

        // Assert
        assertEquals(apiKey, result);
    }

    @Test
    void extractApiKey_ShouldExtractFromAuthorizationHeader() {
        // Arrange
        String apiKey = "sk_testapikey";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + apiKey);
        when(request.getHeader("X-API-Key")).thenReturn(null);

        // Act
        String result = filter.extractApiKey(request);

        // Assert
        assertEquals(apiKey, result);
    }

    @Test
    void extractApiKey_ShouldReturnNull_WhenNoHeadersPresent() {
        // Arrange
        when(request.getHeader("X-API-Key")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(null);

        // Act
        String result = filter.extractApiKey(request);

        // Assert
        assertNull(result);
    }

    @Test
    void extractApiKey_ShouldReturnNull_WhenAuthorizationHeaderMalformed() {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("InvalidFormat");
        when(request.getHeader("X-API-Key")).thenReturn(null);

        // Act
        String result = filter.extractApiKey(request);

        // Assert
        assertNull(result);
    }

    @Test
    void extractApiKey_ShouldReturnNull_WhenAuthorizationHeaderEmpty() {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("Bearer ");
        when(request.getHeader("X-API-Key")).thenReturn(null);

        // Act
        String result = filter.extractApiKey(request);

        // Assert
        assertNull(result);
    }

    @Test
    void setAuthentication_ShouldSetSecurityContext() {
        // Arrange
        String clientId = "client-123";
        var authorities = Arrays.asList("ROLE_USER");
        ApiKeyValidationResult validationResult = ApiKeyValidationResult.valid(
                clientId, authorities, "Test client");

        // Act
        filter.setAuthentication(validationResult);

        // Assert
        verify(securityContext, times(1)).setAuthentication(authenticationCaptor.capture());
        
        Authentication auth = authenticationCaptor.getValue();
        assertNotNull(auth);
        assertEquals(clientId, auth.getName());
        assertEquals(1, auth.getAuthorities().size());
        assertEquals("ROLE_USER", auth.getAuthorities().iterator().next().getAuthority());
        assertTrue(auth.isAuthenticated());
    }

    @Test
    void setAuthentication_ShouldDoNothing_WhenValidationResultIsInvalid() {
        // Arrange
        ApiKeyValidationResult validationResult = ApiKeyValidationResult.invalid("Invalid key");

        // Act
        filter.setAuthentication(validationResult);

        // Assert
        verify(securityContext, never()).setAuthentication(any());
    }

    @Test
    void cleanup_ShouldClearSecurityContextHolder() {
        // Arrange
        SecurityContextHolder.setContext(securityContext);

        // Act
        filter.cleanup();

        // Assert
        SecurityContext clearedContext = SecurityContextHolder.getContext();
        assertNotNull(clearedContext);
        // The context is cleared (no authentication)
        assertNull(clearedContext.getAuthentication());
    }
}