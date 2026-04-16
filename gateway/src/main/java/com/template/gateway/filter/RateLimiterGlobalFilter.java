package com.template.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiting filter for the Gateway.
 *
 * <p>Limits requests per client IP within a sliding time window.
 * In production, consider using Redis-backed rate limiting or
 * Spring Cloud Gateway's built-in RequestRateLimiter with Redis.</p>
 */
@Component
@Slf4j
public class RateLimiterGlobalFilter implements GlobalFilter, Ordered {

    private final ConcurrentHashMap<String, SlidingWindowCounter> counters =
            new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gateway.rate-limit.requests-per-second:10}")
    private int requestsPerSecond;

    @Value("${gateway.rate-limit.enabled:true}")
    private boolean enabled;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {
        if (!enabled) {
            return chain.filter(exchange);
        }

        String clientIp = getClientIp(exchange);
        String path = exchange.getRequest().getURI().getPath();

        // Skip rate limiting for health checks and static resources
        if (shouldSkip(path)) {
            return chain.filter(exchange);
        }

        SlidingWindowCounter counter = counters.computeIfAbsent(
                clientIp + ":" + path,
                k -> new SlidingWindowCounter(requestsPerSecond, 1000));

        if (counter.tryAcquire()) {
            addRateLimitHeaders(exchange, counter);
            return chain.filter(exchange);
        }

        log.warn("Rate limit exceeded for IP: {}, path: {}", clientIp, path);
        return handleRateLimitedResponse(exchange, clientIp, path);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }

    private boolean shouldSkip(String path) {
        return path.startsWith("/manage/") ||
               path.startsWith("/actuator/") ||
               path.endsWith(".css") || path.endsWith(".js") ||
               path.contains("/webjars/");
    }

    private String getClientIp(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest()
                .getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return exchange.getRequest().getRemoteAddress() != null ?
                exchange.getRequest().getRemoteAddress()
                        .getAddress().getHostAddress() : "unknown";
    }

    private void addRateLimitHeaders(ServerWebExchange exchange,
                                     SlidingWindowCounter counter) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().add("X-RateLimit-Limit",
                String.valueOf(requestsPerSecond));
        response.getHeaders().add("X-RateLimit-Remaining",
                String.valueOf(counter.getRemaining()));
        response.getHeaders().add("X-RateLimit-Reset",
                String.valueOf(counter.getWindowEndEpoch()));
    }

    private Mono<Void> handleRateLimitedResponse(ServerWebExchange exchange,
                                                  String clientIp,
                                                  String path) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = new HashMap<>();
        body.put("error", "Too Many Requests");
        body.put("message", "Rate limit exceeded");
        body.put("clientIp", clientIp);
        body.put("path", path);
        body.put("retryAfter", "1");

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory()
                    .wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            byte[] bytes = "{\"error\":\"rate limited\"}"
                    .getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        }
    }

    /**
     * Simple sliding window rate limiter implementation.
     */
    private static class SlidingWindowCounter {

        private final int maxRequests;
        private final long windowMillis;
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart;

        SlidingWindowCounter(int maxRequests, long windowMillis) {
            this.maxRequests = maxRequests;
            this.windowMillis = windowMillis;
            this.windowStart = Instant.now().toEpochMilli();
        }

        synchronized boolean tryAcquire() {
            long now = Instant.now().toEpochMilli();
            if (now - windowStart > windowMillis) {
                this.windowStart = now;
                this.count.set(0);
            }
            if (count.get() < maxRequests) {
                count.incrementAndGet();
                return true;
            }
            return false;
        }

        int getRemaining() {
            return Math.max(0, maxRequests - count.get());
        }

        long getWindowEndEpoch() {
            return (windowStart + windowMillis) / 1000;
        }
    }
}
