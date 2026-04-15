package com.template.microservice.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory redisConnectionFactory;

    @Override
    public Health health() {
        RedisConnection connection = null;
        try {
            connection = redisConnectionFactory.getConnection();
            String pong = connection.ping();
            
            if ("PONG".equals(pong)) {
                return Health.up()
                        .withDetail("redis", "connected")
                        .withDetail("ping", pong)
                        .build();
            }
            
            return Health.down()
                    .withDetail("redis", "unexpected ping response: " + pong)
                    .build();
            
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            return Health.down()
                    .withDetail("redis", "connection failed")
                    .withDetail("error", e.getMessage())
                    .build();
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }
}