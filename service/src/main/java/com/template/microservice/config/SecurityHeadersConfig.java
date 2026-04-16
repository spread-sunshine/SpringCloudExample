package com.template.microservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.header.HeaderWriter;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.header.writers.*;

import java.util.List;

/**
 * Configuration for security HTTP headers.
 * Provides defense against common web vulnerabilities.
 */
@Configuration
public class SecurityHeadersConfig {

    @Bean
    public HeaderWriterFilter headerWriterFilter() {
        List<HeaderWriter> headerWriters = List.of(
                // Content Security Policy
                new ContentSecurityPolicyHeaderWriter(
                        "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                        "style-src 'self' 'unsafe-inline'; " +
                        "img-src 'self' data: https:; " +
                        "font-src 'self' data:; " +
                        "connect-src 'self'; " +
                        "frame-ancestors 'none'; " +
                        "base-uri 'self'; " +
                        "form-action 'self'"
                ),
                // HTTP Strict Transport Security
                new HstsHeaderWriter(),
                // X-Content-Type-Options
                new XContentTypeOptionsHeaderWriter(),
                // Referrer Policy
                new ReferrerPolicyHeaderWriter(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN),
                // Feature Policy / Permissions Policy
                (request, response) -> {
                    response.setHeader("Permissions-Policy",
                            "camera=(), microphone=(), geolocation=(), payment=()");
                    response.setHeader("Feature-Policy",
                            "camera 'none'; microphone 'none'; geolocation 'none'; payment 'none'");
                },
                // Cache control for sensitive endpoints
                (request, response) -> {
                    if (request.getRequestURI().contains("/api/auth") ||
                            request.getRequestURI().contains("/api/admin")) {
                        response.setHeader(HttpHeaders.CACHE_CONTROL,
                                "no-store, no-cache, must-revalidate, max-age=0");
                        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
                        response.setHeader(HttpHeaders.EXPIRES, "0");
                    }
                }
        );

        return new HeaderWriterFilter(headerWriters);
    }
}