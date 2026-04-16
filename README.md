# Spring Cloud Microservice Template

A reusable **multi-module** microservice template following industry best practice:
**Gateway (WebFlux/reactive) + Service (Servlet/JPA)** separation.
Comprehensive security including API key authentication, JWT, role-based access control,
and full observability stack.

## Architecture Overview

```
                        ┌─────────────────────────────┐
                        │      External Clients        │
                        │  (Browser / Frontend / API)  │
                        └──────────┬──────────────────┘
                                   │ :8081
                                   ▼
┌─────────────────────────────────────────────────────────┐
│              microservice-gateway (WebFlux)             │
│  Port: 8081 | Reactive Stack | Independent Process     │
│                                                          │
│  ├── GlobalCorsConfig        CORS configuration          │
│  ├── GatewayRouteConfig      Route: /api/** → lb://service│
│  ├── RequestLoggingFilter    Request/response logging    │
│  ├── RateLimiterGlobalFilter In-memory rate limiting     │
│  └── GatewayErrorHandler     JSON error responses        │
└──────────────────┬──────────────────────────────────────┘
                   │ lb://microservice-template (Eureka)
                   │ OR http://localhost:8080 (dev static)
                   ▼
┌─────────────────────────────────────────────────────────┐
│           microservice-service (Servlet + JPA)          │
│  Port: 8080 | Servlet Stack | Business Logic            │
│                                                          │
│  ├── SecurityConfig         JWT + API Key auth          │
│  ├── AuthController          /api/auth/* endpoints       │
│  ├── ExampleController        /api/example/* endpoints   │
│  ├── RateLimitService         Redis-aware rate limiting   │
│  ├── Service layer            JPA/Hibernate/Redis/Kafka  │
│  └── ErrorController          JSON error handler          │
└─────────────────────────────────────────────────────────┘
```

### Why This Architecture?

| Approach | Description | Verdict |
|----------|-------------|---------|
| **Option A**: Embed Gateway | `spring-boot-starter-gateway` conflicts with `spring-boot-starter-web` (WebFlux vs Servlet) | **Not possible** |
| **Option B**: Migrate all to WebFlux | Rewrite ~60 files (JPA→R2DBC, Servlet Filters→WebFilters, etc.) | **High risk, high cost** |
| **Option C (chosen)**: Split modules | Gateway = WebFlux (new), Service = Servlet (unchanged) | **Industry standard, minimal change** |

> See [WebFlux vs Servlet.md](./WebFlux%20vs%20Servlet.md) for detailed comparison.

---

## Features

- **Spring Boot 3.2.5** with **Spring Cloud 2023.0.3**
- **Multi-Module Architecture**: Gateway (reactive) + Service (servlet)
- **Dual Authentication**: API Key + JWT token support (in Service module)
- **Role-Based Access Control**: Fine-grained authorization with Spring Security
- **API Key Management**: Full lifecycle management (creation, validation, rotation, revocation)
- **Spring Cloud Gateway**: Routing, load balancing, circuit breaker, rate limiting
- **Service Discovery**: Netflix Eureka client integration
- **Circuit Breaker**: Resilience4j with bulkhead, retry, rate limiting (Gateway + Service)
- **Distributed Tracing**: Micrometer Tracing with Zipkin integration
- **Declarative REST Client**: OpenFeign
- **API Documentation**: OpenAPI 3 with Swagger UI (Service module)
- **Database Support**: PostgreSQL (production), H2 (development), Spring Data JPA, Flyway
- **Caching**: Redis + Caffeine with graceful degradation
- **Message Queues**: RabbitMQ and Kafka support
- **Monitoring**: Prometheus metrics, Grafana dashboards, custom health indicators
- **Containerization**: Multi-stage Dockerfiles per module, docker-compose orchestration

## Technology Stack

| Module | Stack | Key Components |
|--------|-------|---------------|
| **Gateway** | WebFlux (Reactive) | Spring Cloud Gateway, Reactor Netty, LoadBalancer |
| **Service** | Servlet (MVC) | Spring MVC, Spring Data JPA, Spring Security, Redis, Kafka, RabbitMQ |
| **Shared** | Java 17, Maven, Spring Boot 3, Spring Cloud 2023.0.3 | Micrometer, Zipkin/Brave |

## Project Structure

```
TestSprintCloud/                          # Root (parent POM, packaging=pom)
├── pom.xml                               # Parent POM: dependency management + build plugins
│                                          # Modules: service, gateway
│
├── service/                              # ★ Business Service Module (Servlet + JPA)
│   ├── pom.xml                           #   All original dependencies preserved
│   └── src/
│       ├── main/java/com/template/microservice/
│       │   ├── Application.java          #     @SpringBootApplication :8080
│       │   ├── config/
│       │   │   ├── SecurityConfig.java   #     Servlet Security filter chain
│       │   │   ├── MetricsConfig.java    #     23+ custom Micrometer metrics
│       │   │   ├── CacheConfig.java      #     Caffeine + Redis cache
│       │   │   ├── OpenApiConfig.java    #     Swagger/OpenAPI config
│       │   │   ├── KafkaConfig.java      #     Kafka producer/consumer
│       │   │   ├── RabbitMQConfig.java   #     RabbitMQ config
│       │   │   └── ...
│       │   ├── controller/
│       │   │   ├── AuthController.java   #     POST /api/auth/{login,register,...}
│       │   │   ├── ExampleController.java#     GET/POST /api/example/*
│       │   │   └── ErrorController.java  #     Custom JSON error handler
│       │   ├── security/
│       │   │   ├── JwtAuthenticationFilter.java
│       │   │   ├── ApiKeyAuthenticationFilter.java
│       │   │   ├── JwtTokenProvider.java
│       │   │   └── ... (10 security classes)
│       │   ├── service/
│       │   │   ├── AuthService.java
│       │   │   ├── RateLimitService.java #     Redis-aware, graceful degradation
│       │   │   └── message/              #     KafkaConsumer, RabbitMQProducer...
│       │   ├── filter/
│       │   │   ├── RequestLoggingFilter.java
│       │   │   └── RateLimitFilter.java  #     Excludes swagger/static paths
│       │   ├── model/entity/dto/        #     User, Role, LoginRequest, etc.
│       │   ├── repository/              #     UserRepository, RoleRepository
│       │   ├── exception/               #     GlobalExceptionHandler
│       │   ├── aspect/                  #     ApiResponseAspect
│       │   ├── client/                  #     ExampleFeignClient
│       │   ├── health/                  #     DatabaseHealthIndicator
│       │   └── util/                    #     IdGenerator, SecurityUtils
│       └── main/resources/
│           ├── application.yml          #     Base config
│           ├── application-dev.yml      #     Dev: H2, Caffeine, no Eureka
│           └── application-prod.yml     #     Prod: PostgreSQL, full stack
│
├── gateway/                             # ★ Gateway Module (WebFlux, NEW)
│   ├── pom.xml                           #   spring-cloud-starter-gateway + loadbalancer
│   └── src/
│       ├── main/java/com/template/gateway/
│       │   ├── GatewayApplication.java  #     @SpringBootApplication :8081
│       │   ├── config/
│       │   │   ├── GatewayRouteConfig.java   #   RouteLocator: /api/** → lb://service
│       │   │   └── GlobalCorsConfig.java     #   CORS for reactive stack
│       │   └── filter/
│       │       ├── RequestLoggingFilter.java  #   Global request logging
│       │       ├── RateLimiterGlobalFilter.java # In-memory sliding window
│       │       └── GatewayErrorHandler.java    #   JSON error via onErrorResume
│       └── main/resources/
│           ├── application.yml          #     Default: Eureka discovery routing
│           ├── application-dev.yml      #     Dev: static routes to localhost:8080
│           └── application-prod.yml     #     Prod: Circuit Breaker + Retry
│
├── Dockerfile.service                   # Multi-stage build for service module
├── Dockerfile.gateway                   # Multi-stage build for gateway module
├── docker-compose.yml                   # Full orchestration: gateway → service + infra
├── docker-compose.prod.yml              # Production compose override
├── k8s/                                 # Kubernetes deployment manifests
├── prometheus/ grafana/                 # Monitoring dashboards & alerts
└── WebFlux vs Servlet.md                # Architecture decision document
```

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose (optional, for full infrastructure)

### Building

```bash
# Build all modules (parent + service + gateway)
mvn clean package

# Build only specific module
mvn clean package -pl service
mvn clean package -pl gateway
```

### Running

#### Option 1: Service Only (Development)

Run the business service directly — ideal for development without Gateway:

```bash
# Start service on port 8080 (dev profile, H2 database, no external deps)
mvn spring-boot:run -pl service -Dspring-boot.run.profiles=dev
```

Access:
| Endpoint | URL |
|----------|-----|
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| Actuator Health | `http://localhost:8080/manage/health` |
| API (auth required) | `http://localhost:8080/api/example/public` |

#### Option 2: Gateway + Service (Development)

Run both modules together for a realistic microservices setup:

```bash
# Terminal 1: Start service on port 8080
mvn spring-boot:run -pl service -Dspring-boot.run.profiles=dev

# Terminal 2: Start gateway on port 8081 (static route to localhost:8080)
mvn spring-boot:run -pl gateway -Dspring-boot.run.profiles=dev
```

All traffic goes through Gateway at port 8081:

| Via Gateway (port 8081) | Direct (port 8080) |
|--------------------------|---------------------|
| `http://localhost:8081/api/**` | `http://localhost:8080/api/**` |
| `http://localhost:8081/swagger-ui.html` | `http://localhost:8080/swagger-ui.html` |
| `http://localhost:8081/manage/health` | `http://localhost:8080/manage/health` |

#### Option 3: Full Docker Compose (Production-like)

```bash
# Build and start all services (Gateway + Service + Infrastructure)
docker compose up --build

# Or just the core two services
docker compose up --build microservice-gateway microservice-template postgres redis
```

### Using API Key Authentication

```bash
# Default keys configured in application-dev.yml:
#   test-api-key-1 → roles: ROLE_USER, ROLE_ADMIN
#   test-api-key-2 → roles: ROLE_USER

# Through Gateway
curl -H "X-API-Key: test-api-key-1" http://localhost:8081/api/example/1

# Direct to Service
curl -H "X-API-Key: test-api-key-1" http://localhost:8080/api/example/1
```

## Configuration

### Profiles

| Profile | Gateway Behavior | Service Behavior | External Dependencies |
|---------|------------------|------------------|----------------------|
| **dev** (default) | Static route to `localhost:8080`, Eureka disabled, rate limiter disabled | H2 in-memory DB, Caffeine cache, Eureka disabled, debug logging | None required |
| **prod** | Eureka discovery (`lb://service`), circuit breaker, retry, rate limiting (50 req/s) | PostgreSQL, Redis cache, RabbitMQ, Kafka, full observability | PostgreSQL, Eureka, Redis, RabbitMQ, Kafka, Zipkin |

Activate profile:
```bash
mvn spring-boot:run -pl service -Dspring-boot.run.profiles=prod
mvn spring-boot:run -pl gateway -Dspring-boot.run.profiles=prod
```

### Key Configuration Files

| File | Location | Purpose |
|------|----------|---------|
| Root POM | `pom.xml` | Parent aggregator: shared dependency versions, plugin management |
| Service base config | `service/src/main/resources/application.yml` | Shared Spring config, server port, management endpoints |
| Service dev config | `service/src/main/resources/application-dev.yml` | H2 DB, Caffeine cache, debug logging, no external deps |
| Service prod config | `service/src/main/resources/application-prod.yml` | PostgreSQL, Redis, Kafka, full observability |
| Gateway base config | `gateway/src/main/resources/application.yml` | Port 8081, reactive mode, discovery locator |
| Gateway dev config | `gateway/src/main/resources/application-dev.yml` | Static routes to localhost:8080, no Eureka |
| Gateway prod config | `gateway/src/main/resources/application-prod.yml` | Eureka discovery, circuit breaker, retry |

### Development Profile Highlights

**Service (`dev`)**:
- **Eureka**: Disabled — runs independently
- **Database**: H2 in-memory (no setup needed)
- **Cache**: Caffeine local cache (no Redis needed)
- **Redis**: Optional — only used by `RateLimitService`; graceful degradation when unavailable
- **RabbitMQ/Kafka**: Commented out — uncomment when message queue needed
- **Swagger UI**: Excluded from rate limiting for easy development access
- **Actuator**: All endpoints exposed at `/manage/*`

**Gateway (`dev`)**:
- **Eureka**: Disabled — uses static URL routes instead
- **Routes**: All `/api/**`, `/manage/**`, `/swagger*` forwarded to `http://localhost:8080`
- **Rate Limiting**: Disabled for development convenience

## Docker Support

### Building Images

```bash
# Build both module images
docker compose build

# Or build individually
docker build -f Dockerfile.service -t microservice-service:latest .
docker build -f Dockerfile.gateway -t microservice-gateway:latest .
```

### Docker Compose Services

| Service | Image | Port | Description |
|---------|-------|------|-------------|
| **microservice-gateway** | build (Dockerfile.gateway) | 8081 | API Gateway — entry point, routes to services |
| **microservice-template** | build (Dockerfile.service) | 8080 | Core business service (Servlet + JPA) |
| **eureka** | springcloud/eureka | 8761 | Service discovery registry |
| **config-server** | springcloud/config-server | 8888 | Centralized configuration |
| **postgres** | postgres:14-alpine | 5432 | Production database |
| **redis** | redis:7-alpine | 6379 | Cache + distributed rate limiting |
| **rabbitmq** | rabbitmq:3-management-alpine | 5672 / 15672 | Message queue (+ management UI) |
| **kafka** | apache/kafka | 9092 / 9093 | Event streaming (+ JMX) |
| **zookeeper** | confluentinc/cp-zookeeper | 2181 | Kafka coordination |
| **zipkin** | openzipkin/zipkin | 9411 | Distributed tracing UI |
| **prometheus** | prom/prometheus | 9090 | Metrics collection |
| **grafana** | grafana/grafana | 3000 | Visualization dashboard |

### Common Compose Commands

```bash
# Start everything
docker compose up --build -d

# Start only core services (gateway + service + db + cache)
docker compose up -d microservice-gateway microservice-template postgres redis

# Add message queues
docker compose up -d rabbitmq kafka zookeeper

# View logs
docker compose logs -f microservice-gateway
docker compose logs -f microservice-template

# Stop everything
docker compose down
```

## API Documentation

OpenAPI documentation is available on the **Service** module:

| Resource | URL (via Gateway) | URL (Direct) |
|----------|-------------------|--------------|
| Swagger UI | `http://localhost:8081/swagger-ui.html` | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8081/v3/api-docs` | `http://localhost:8080/v3/api-docs` |
| OpenAPI YAML | `http://localhost:8081/v3/api-docs.yaml` | `http://localhost:8080/v3/api-docs.yaml` |

## Testing

### Running Tests

```bash
# Test all modules
mvn test

# Test only service module
mvn test -pl service

# Test only gateway module
mvn test -pl gateway

# Run specific test class
mvn test -pl service -Dtest=ApiKeyManagementServiceTest

# Integration tests (requires Docker/Testcontainers)
mvn verify -pl service
```

### Test Coverage

- **Unit tests**: Service layer, security components, utilities (~12 test classes)
- **Integration tests**: Controller endpoints with embedded database
- **Security tests**: Authentication and authorization flows (JWT, API Key)

## Monitoring and Observability

### Actuator Endpoints

Both modules expose actuator endpoints under `/manage/`:

| Endpoint | Service (8080) | Gateway (8081) |
|----------|-----------------|----------------|
| `/manage/health` | App health (DB, Redis) | Gateway health (routes) |
| `/manage/info` | App info | Gateway info |
| `/manage/metrics` | JVM, HTTP, cache metrics | Route/filter metrics |
| `/manage/prometheus` | Prometheus exporter | Gateway metrics exporter |
| `/manage/gateway/routes` | N/A | Dynamic route management |

### Distributed Tracing

Both Gateway and Service emit tracing spans to Zipkin:
- Access Zipkin UI: `http://localhost:9411`
- Trace flows: **Client → Gateway → Service → Database**

## Deployment

### Kubernetes

Kubernetes manifests are provided in `k8s/` directory:

```bash
kubectl apply -f k8s/
```

Includes: Deployment, Service, ConfigMap, Secret, ServiceAccount, HPA, Ingress

### Adding New Microservices

This template is designed to be extended:

1. **Add a new service module** under the parent POM
2. **Register with Eureka** (or add static route in Gateway)
3. **Update `GatewayRouteConfig.java`** to add routing rules
4. **Update docker-compose.yml** if deploying locally

## Troubleshooting

### Common Issues

1. **Service won't start**:
   - Check Java version: `java -version` (must be 17+)
   - Check port availability: `netstat -an | findstr :8080`
   - Dev profile needs **no external dependencies** — H2 is embedded

2. **Gateway won't start** (port conflict):
   - Ensure port 8081 is free: `netstat -an | findstr :8081`
   - Dev profile routes to `localhost:8080` — ensure Service is running first

3. **Swagger UI returns 500 error**:
   - Most likely caused by Redis being unreachable (rate limiter)
   - `RateLimitService` degrades gracefully — check `shouldNotFilter()` excludes swagger paths
   - If using through Gateway, verify Gateway's static route includes `/swagger*`

4. **API key authentication failing**:
   - Verify key format in `application-dev.yml` under `security.api.keys`
   - Check roles assigned to each key

5. **Redis connection errors in dev**:
   - **Expected behavior** — app still works, rate limiting degrades gracefully
   - To enable full rate limiting: `docker compose up -d redis`

6. **Multi-module build issues**:
   - Always build from root: `mvn clean package` (not from submodule)
   - Submodule inherits parent's dependency management automatically

7. **Docker Compose issues**:
   - Ensure Docker daemon is running
   - Check port conflicts across all services (ports: 3000-9411)
   - View logs: `docker compose logs <service-name>`

### Logs

| Location | Level | Content |
|----------|-------|---------|
| Console | DEV: DEBUG / PROD: INFO | Real-time output |
| `logs/microservice-template-dev.log` | DEBUG | Service module log file |
| Console (gateway) | DEBUG | Gateway request/response logs |

## License

MIT License

## Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure `mvn clean compile && mvn test` passes
5. Submit a pull request
