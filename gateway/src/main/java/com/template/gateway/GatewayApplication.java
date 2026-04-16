package com.template.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Gateway Application entry point.
 *
 * <p>This is a standalone Spring Cloud Gateway (WebFlux) service that
 * runs on port 8081 and routes external traffic to backend services
 * registered with Eureka.</p>
 *
 * <p>Architecture: Gateway (reactive) -> Service(s) (servlet)</p>
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
