package com.template.microservice.filter;

import com.template.microservice.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(2) // After security filter
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String clientId = getClientIdentifier(request);
        String endpoint = request.getRequestURI();
        
        if (rateLimitService.isRateLimited(clientId, endpoint)) {
            log.warn("Rate limit exceeded for client: {}, endpoint: {}", clientId, endpoint);
            
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                    "{\"error\":\"Rate limit exceeded\",\"client\":\"%s\",\"endpoint\":\"%s\"}",
                    clientId, endpoint));
            return;
        }
        
        // Add rate limit headers
        RateLimitService.RateLimitInfo rateLimitInfo = rateLimitService.getRateLimitInfo(clientId, endpoint);
        response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimitInfo.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(rateLimitInfo.getRemaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(rateLimitInfo.getResetTime()));
        
        filterChain.doFilter(request, response);
    }

    private String getClientIdentifier(HttpServletRequest request) {
        // Try to get client IP
        String clientIp = request.getRemoteAddr();
        
        // If behind proxy
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            clientIp = xForwardedFor.split(",")[0].trim();
        }
        
        // For authenticated users, use user ID for more granular rate limiting
        String userId = request.getUserPrincipal() != null ? 
                request.getUserPrincipal().getName() : null;
        
        if (userId != null) {
            return "user:" + userId;
        }
        
        return "ip:" + clientIp;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip rate limiting for health checks and actuator endpoints
        String path = request.getRequestURI();
        return path.startsWith("/actuator/") || 
               path.startsWith("/manage/") ||
               path.equals("/api/auth/login") ||
               path.equals("/api/auth/register");
    }
}