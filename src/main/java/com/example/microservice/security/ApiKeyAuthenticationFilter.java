package com.example.microservice.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * API Key authentication filter for authenticating requests using API keys.
 * The API key should be provided in the X-API-Key header.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyValidator apiKeyValidator;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String apiKey = resolveApiKey(request);
            
            if (StringUtils.hasText(apiKey)) {
                Authentication authentication = authenticate(apiKey);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Set API Key authentication to security context, uri: {}", request.getRequestURI());
            }
        } catch (BadCredentialsException ex) {
            log.warn("Invalid API Key: {}", ex.getMessage());
            // Continue filter chain - other authentication methods might be used
        } catch (Exception ex) {
            log.error("API Key authentication error", ex);
        }
        
        filterChain.doFilter(request, response);
    }

    private String resolveApiKey(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        if (!StringUtils.hasText(apiKey)) {
            apiKey = request.getHeader("Authorization");
            if (StringUtils.hasText(apiKey) && apiKey.startsWith("ApiKey ")) {
                apiKey = apiKey.substring(7);
            } else {
                apiKey = null;
            }
        }
        return apiKey;
    }

    private Authentication authenticate(String apiKey) {
        ApiKeyValidationResult validationResult = apiKeyValidator.validate(apiKey);
        if (!validationResult.isValid()) {
            throw new BadCredentialsException("Invalid API Key");
        }
        
        List<SimpleGrantedAuthority> authorities = validationResult.getAuthorities().stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        
        return new ApiKeyAuthenticationToken(apiKey, authorities);
    }
}