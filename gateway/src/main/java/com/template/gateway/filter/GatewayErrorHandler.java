package com.template.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global error handler for the Gateway (WebFlux).
 *
 * <p>Implements error handling via {@code onErrorResume} on the filter chain,
 * catching all exceptions from downstream services and returning consistent
 * JSON error responses.</p>
 */
@Component
@Slf4j
public class GatewayErrorHandler implements GlobalFilter, Ordered {

    private static final int ORDER = Ordered.LOWEST_PRECEDENCE;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {
        return chain.filter(exchange)
                .onErrorResume(Throwable.class, ex ->
                        handleError(exchange, ex));
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    private Mono<Void> handleError(ServerWebExchange exchange,
                                    Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        HttpStatus status = resolveHttpStatus(ex);
        Map<String, Object> body = buildErrorBody(status, ex, exchange);

        log.error("[GATEWAY ERROR] Path: {}, Status: {} | Type: {} | Msg: {}",
                exchange.getRequest().getURI().getPath(),
                status.value(),
                ex.getClass().getSimpleName(),
                ex.getMessage());

        response.setStatusCode(status);
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_JSON_VALUE);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            byte[] bytes =
                    "{\"success\":false,\"error\":\"Internal Server Error\"}"
                            .getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        }
    }

    private HttpStatus resolveHttpStatus(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            return (HttpStatus) rse.getStatusCode();
        }
        if (ex instanceof org.springframework.cloud.gateway.support
                        .NotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (ex instanceof org.springframework.cloud.gateway.support
                        .ServiceUnavailableException) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        if (ex instanceof org.springframework.web.server
                        .ResponseStatusException) {
            return HttpStatus.valueOf(
                    ((org.springframework.web.server.ResponseStatusException)
                            ex).getStatusCode().value());
        }
        // Check if it's already an HTTP status wrapped exception
        String className = ex.getClass().getName();
        if (className.contains("BadGateway")) {
            return HttpStatus.BAD_GATEWAY;
        }
        if (className.contains("Timeout") || className.contains(
                "TimeoutException")) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private Map<String, Object> buildErrorBody(HttpStatus status,
                                               Throwable ex,
                                               ServerWebExchange exchange) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", ex.getMessage() != null ?
                ex.getMessage() : "An unexpected error occurred");
        body.put("path",
                exchange.getRequest().getURI().getPath());
        body.put("timestamp", Instant.now().toString());
        body.put("errorCode", mapErrorCode(status));
        return body;
    }

    private String mapErrorCode(HttpStatus status) {
        switch (status) {
            case TOO_MANY_REQUESTS:
                return "RATE_LIMIT_EXCEEDED";
            case NOT_FOUND:
                return "NOT_FOUND";
            case SERVICE_UNAVAILABLE:
                return "SERVICE_UNAVAILABLE";
            case BAD_GATEWAY:
                return "BAD_GATEWAY";
            case GATEWAY_TIMEOUT:
                return "GATEWAY_TIMEOUT";
            case UNAUTHORIZED:
                return "UNAUTHORIZED";
            case FORBIDDEN:
                return "FORBIDDEN";
            default:
                return "INTERNAL_ERROR";
        }
    }
}
