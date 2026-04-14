package com.example.microservice.filter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
@Order(1) // First filter
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        Instant startTime = Instant.now();
        String requestId = UUID.randomUUID().toString();
        
        // Wrap request to allow reading body multiple times
        HttpServletRequest wrappedRequest = new ContentCachingRequestWrapper(request);
        
        // Set MDC for logging context
        MDC.put("requestId", requestId);
        MDC.put("clientIp", getClientIp(wrappedRequest));
        MDC.put("userAgent", wrappedRequest.getHeader("User-Agent"));
        
        try {
            // Log request
            logRequest(wrappedRequest, requestId);
            
            // Continue filter chain
            filterChain.doFilter(wrappedRequest, response);
            
        } finally {
            // Log response
            Instant endTime = Instant.now();
            long duration = Duration.between(startTime, endTime).toMillis();
            logResponse(wrappedRequest, response, requestId, duration);
            
            // Clear MDC
            MDC.clear();
        }
    }

    private void logRequest(HttpServletRequest request, String requestId) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("Incoming Request [").append(requestId).append("]: ");
        logMessage.append(request.getMethod()).append(" ").append(request.getRequestURI());
        
        String queryString = request.getQueryString();
        if (queryString != null) {
            logMessage.append("?").append(queryString);
        }
        
        logMessage.append(" from ").append(getClientIp(request));
        logMessage.append(" (User-Agent: ").append(request.getHeader("User-Agent")).append(")");
        
        // Log headers for debugging (avoid logging sensitive headers)
        if (log.isDebugEnabled()) {
            request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
                if (!isSensitiveHeader(headerName)) {
                    log.debug("Header [{}]: {}", headerName, request.getHeader(headerName));
                }
            });
        }
        
        log.info(logMessage.toString());
    }

    private void logResponse(HttpServletRequest request, HttpServletResponse response, 
                            String requestId, long duration) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("Outgoing Response [").append(requestId).append("]: ");
        logMessage.append(request.getMethod()).append(" ").append(request.getRequestURI());
        logMessage.append(" -> ").append(response.getStatus());
        logMessage.append(" in ").append(duration).append("ms");
        
        if (response.getStatus() >= 400) {
            log.warn(logMessage.toString());
        } else {
            log.info(logMessage.toString());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private boolean isSensitiveHeader(String headerName) {
        String lowerHeader = headerName.toLowerCase();
        return lowerHeader.contains("authorization") ||
               lowerHeader.contains("cookie") ||
               lowerHeader.contains("password") ||
               lowerHeader.contains("secret") ||
               lowerHeader.contains("token") ||
               lowerHeader.contains("key");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip logging for health checks
        String path = request.getRequestURI();
        return path.startsWith("/actuator/health") || 
               path.startsWith("/manage/health");
    }
}