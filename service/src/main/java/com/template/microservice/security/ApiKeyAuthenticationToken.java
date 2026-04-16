package com.template.microservice.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Authentication token for API key authentication.
 */
public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {
    
    private final String apiKey;
    private final Object principal;
    
    public ApiKeyAuthenticationToken(String apiKey, Collection<? extends GrantedAuthority> authorities) {
        this(apiKey, apiKey, authorities);
    }
    
    public ApiKeyAuthenticationToken(Object principal, String apiKey, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.apiKey = apiKey;
        this.principal = principal;
        super.setAuthenticated(true);
    }
    
    @Override
    public Object getCredentials() {
        return apiKey;
    }
    
    @Override
    public Object getPrincipal() {
        return principal;
    }
}