package com.template.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Global request/response logging filter for the Gateway.
 *
 * <p>Logs incoming request details (method, path, client IP, headers)
 * and response status codes. Runs at the highest precedence to capture
 * all requests passing through the gateway.</p>
 */
@Component
@Slf4j
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final String START_TIME = "gateway.startTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String requestId = exchange.getRequest().getId();
        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        String clientIp = request.getRemoteAddress() != null ?
                request.getRemoteAddress().getAddress().getHostAddress() : "unknown";

        log.info("[GATEWAY] Request: {} {} | ID: {} | IP: {} | From: {}",
                method, path, requestId, clientIp,
                request.getHeaders().getOrigin());

        exchange.getAttributes().put(START_TIME, System.currentTimeMillis());

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long startTime = exchange.getAttributeOrDefault(
                    START_TIME, System.currentTimeMillis());
            long duration = System.currentTimeMillis() - startTime;
            int statusCode = exchange.getResponse()
                    .getStatusCode() != null ?
                    exchange.getResponse().getStatusCode().value() : 0;

            log.info("[GATEWAY] Response: {} {} | Status: {} | Duration: {}ms",
                    method, path, statusCode, duration);

            if (statusCode >= 500) {
                log.error("[GATEWAY] Server error: {} {} | Status: {}",
                        method, path, statusCode);
            } else if (statusCode >= 400) {
                log.warn("[GATEWAY] Client error: {} {} | Status: {}",
                        method, path, statusCode);
            }
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
