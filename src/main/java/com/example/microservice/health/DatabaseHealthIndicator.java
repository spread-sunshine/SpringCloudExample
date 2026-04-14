package com.example.microservice.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Health health() {
        try {
            // Simple query to check database connectivity
            Map<String, Object> result = jdbcTemplate.queryForMap("SELECT 1 as status");
            
            if (result != null && result.containsKey("status")) {
                return Health.up()
                        .withDetail("database", "connected")
                        .withDetail("query", "SELECT 1 executed successfully")
                        .build();
            }
            
            return Health.down()
                    .withDetail("database", "unexpected response")
                    .build();
            
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return Health.down()
                    .withDetail("database", "connection failed")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}