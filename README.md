# Spring Cloud Microservice Template

A reusable template for Spring Cloud microservices with comprehensive security including API key authentication, JWT, role-based access control, and full observability stack.

## Features

- **Spring Boot 3.2.5** with **Spring Cloud 2023.0.3**
- **Dual Authentication**: API Key + JWT token support
- **Role-Based Access Control**: Fine-grained authorization with Spring Security
- **API Key Management**: Full lifecycle management (creation, validation, rotation, revocation)
- **Security**: Spring Security with OAuth2 resource server, CORS, CSRF protection, security headers
- **Service Discovery**: Netflix Eureka client integration (optional in dev)
- **Configuration Management**: Spring Cloud Config client support
- **API Gateway**: Spring Cloud Gateway support
- **Circuit Breaker**: Resilience4j with bulkhead, retry, rate limiting
- **Distributed Tracing**: Micrometer Tracing with Zipkin integration
- **Declarative REST Client**: OpenFeign
- **API Documentation**: OpenAPI 3 with Swagger UI
- **Actuator Endpoints**: Health, metrics, info, Prometheus exporter
- **Database Support**: PostgreSQL (production), H2 (development), Spring Data JPA, Flyway migrations
- **Caching**: Redis with Spring Cache abstraction (graceful degradation when unavailable)
- **Message Queues**: RabbitMQ and Kafka support
- **Logging**: Structured JSON logging with Logstash encoder
- **Monitoring**: Prometheus metrics, Grafana dashboards, custom health indicators
- **Containerization**: Multi-stage Dockerfile, docker-compose for full stack
- **Kubernetes**: Deployment manifests for k8s
- **Testing**: Comprehensive unit, integration, and security tests with Testcontainers
- **Multi-environment**: dev, prod profiles with environment-specific configurations

## Technology Stack

- **Java 17**, **Maven**, **Spring Boot 3**, **Spring Cloud**
- **Security**: Spring Security, JWT (jjwt), OAuth2, API Key authentication
- **Database**: PostgreSQL, H2, Spring Data JPA, Flyway
- **Cache**: Redis, Caffeine
- **Messaging**: RabbitMQ, Kafka
- **Monitoring**: Micrometer, Prometheus, Grafana, Zipkin
- **Documentation**: Springdoc OpenAPI
- **Testing**: JUnit 5, Mockito, Testcontainers, Spring Boot Test
- **Container**: Docker, Docker Compose, Kubernetes

## Project Structure

```
src/main/java/com/template/microservice/
├── Application.java                  # Main Spring Boot application
├── config/                           # Configuration classes
│   ├── SecurityConfig.java           # Spring Security configuration
│   ├── SecurityHeadersConfig.java    # Security headers (HSTS, CSP)
│   ├── MetricsConfig.java            # Micrometer metrics configuration
│   ├── RedisConfig.java              # Redis cache configuration
│   ├── RabbitMQConfig.java           # RabbitMQ configuration
│   ├── KafkaConfig.java              # Kafka configuration
│   ├── CacheConfig.java              # Caching configuration
│   ├── FeignConfig.java              # OpenFeign configuration
│   ├── OpenApiConfig.java            # OpenAPI configuration
│   └── SwaggerConfig.java            # Swagger UI configuration
├── controller/                       # REST controllers
│   ├── ErrorController.java          # Custom error handler (JSON responses)
│   ├── ExampleController.java        # Example REST endpoints
│   └── AuthController.java           # Authentication endpoints
├── service/                          # Business services
│   ├── ExampleService.java           # Example business logic
│   ├── AuthService.java              # Authentication service
│   ├── RateLimitService.java         # Rate limiting service (Redis-aware)
│   ├── MetricsService.java           # Custom metrics service
│   └── message/                      # Message queue services
│       ├── RabbitMQProducer.java
│       ├── RabbitMQConsumer.java
│       ├── KafkaProducer.java
│       └── KafkaConsumer.java
├── security/                         # Security components
│   ├── ApiKeyAuthenticationFilter.java    # API key authentication filter
│   ├── ApiKeyManagementService.java       # API key lifecycle management
│   ├── ApiKeyValidator.java               # API key validation interface
│   ├── DatabaseApiKeyValidator.java       # Database-backed validator
│   ├── SimpleApiKeyValidator.java         # In-memory validator
│   ├── ApiKeyValidationResult.java        # Validation result DTO
│   ├── ApiKeyAuthenticationToken.java     # Authentication token
│   ├── JwtTokenProvider.java              # JWT token generation/validation
│   ├── JwtAuthenticationFilter.java       # JWT authentication filter
│   ├── JwtBlacklistService.java           # JWT blacklist management
│   ├── UserDetailsServiceImpl.java        # User details service
│   └── AuditService.java                  # Security audit logging
├── repository/                       # Data access layer
│   ├── UserRepository.java           # User repository
│   └── RoleRepository.java           # Role repository
├── model/                            # Data models
│   ├── entity/                       # JPA entities
│   │   ├── User.java
│   │   └── Role.java
│   ├── dto/                          # Data transfer objects
│   │   ├── LoginRequest.java
│   │   ├── LoginResponse.java
│   │   ├── RegisterRequest.java
│   │   ├── RefreshTokenRequest.java
│   │   └── ApiResponse.java
│   ├── ExampleRequest.java
│   └── ExampleResponse.java
├── client/                           # Feign clients
│   └── ExampleFeignClient.java
├── filter/                           # HTTP filters
│   ├── RequestLoggingFilter.java     # Request logging
│   └── RateLimitFilter.java          # Rate limiting filter (excludes swagger)
├── aspect/                           # AOP aspects
│   └── ApiResponseAspect.java        # Standardized API response wrapping
├── exception/                        # Exception handling
│   ├── GlobalExceptionHandler.java   # Global exception handler (@RestControllerAdvice)
│   ├── BusinessException.java        # Business exception
│   └── ErrorCode.java                # Error codes
├── health/                           # Custom health indicators
│   ├── DatabaseHealthIndicator.java
│   └── RedisHealthIndicator.java
└── util/                             # Utilities
    ├── ValidationUtils.java
    └── IdGenerator.java

src/test/java/com/template/microservice/          # Test suite
├── controller/                       # Controller tests
├── service/                          # Service tests
├── security/                         # Security tests
│   ├── ApiKeyAuthenticationFilterTest.java
│   ├── ApiKeyManagementServiceTest.java
│   ├── DatabaseApiKeyValidatorTest.java
│   ├── AuditServiceTest.java
│   ├── JwtTokenProviderTest.java
│   └── JwtBlacklistServiceTest.java
└── integration/                      # Integration tests
    ├── BaseIntegrationTest.java
    ├── ExampleControllerIntegrationTest.java
    └── AuthIntegrationTest.java
```

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose (optional, for full stack)
- PostgreSQL (optional, H2 used by default in dev)

### Building and Running

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd TestSprintCloud
   ```

2. **Build the project**:
   ```bash
   mvn clean package
   ```

3. **Run locally (development profile)**:
   ```bash
   mvn spring-boot:run
   ```
   
   > **Note**: The development profile (`dev`) is designed to work **without external dependencies**.
   > Eureka, Redis, RabbitMQ, and Kafka are all disabled by default.
   > Rate limiting gracefully degrades when Redis is unavailable.
   > The application will start successfully using the H2 in-memory database.

4. **Access endpoints**:
   - API: `http://localhost:8080/api/example/{id}`
   - Authentication: `http://localhost:8080/api/auth/login`
   - Swagger UI: `http://localhost:8080/swagger-ui.html`
   - Actuator Health: `http://localhost:8080/manage/health`
   - Prometheus Metrics: `http://localhost:8080/manage/prometheus`

### Using API Key Authentication

1. **Default API keys** (configured in `application-dev.yml`):
   - `test-api-key-1` with roles `ROLE_USER,ROLE_ADMIN`
   - `test-api-key-2` with role `ROLE_USER`

2. **Make authenticated requests**:
   ```bash
   # Using X-API-Key header
   curl -H "X-API-Key: test-api-key-1" http://localhost:8080/api/example/1
   
   # Using Authorization header
   curl -H "Authorization: ApiKey test-api-key-1" http://localhost:8080/api/example/1
   ```

## Configuration

### Profiles

| Profile | Description | External Dependencies |
|---------|-------------|----------------------|
| **dev** | Development (default) | None required - H2 in-memory DB, Eureka disabled, Redis optional |
| **prod** | Production | PostgreSQL, Eureka cluster, Redis, RabbitMQ, Kafka, Zipkin |

Activate a profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Key Configuration Files

| File | Purpose |
|------|---------|
| `application.yml` | Base configuration (shared across all profiles) |
| `application-dev.yml` | Development settings - H2 DB, debug logging, no external deps |
| `application-prod.yml` | Production settings - PostgreSQL, optimized, full observability |

### Development Profile Highlights

The `dev` profile is designed for standalone development:

- **Eureka**: Disabled (`eureka.client.enabled=false`)
- **Database**: H2 in-memory (no setup needed)
- **Redis**: Configured but **optional** - rate limiting degrades gracefully when unavailable
- **RabbitMQ/Kafka**: Configured but not required for startup
- **Error Handling**: Custom `ErrorController` returns JSON instead of Whitelabel pages
- **Swagger UI**: Excluded from rate limiting and tracing for easy development access
- **Actuator**: All endpoints exposed at `/manage/*`

### Security Configuration

Key security properties in `application-dev.yml`:

```yaml
security:
  api:
    keys: "test-api-key-1:ROLE_USER,ROLE_ADMIN:Test Client 1;test-api-key-2:ROLE_USER:Test Client 2"
    validator: "database" # simple or database
    key:
      expiration:
        days: 90
      warning:
        days: 7
      length: 32
      prefix: "sk_"
  jwt:
    secret: your-secret-key-change-in-production
    expiration: 86400000 # 24 hours
    blacklist:
      enabled: true
```

### Database Configuration

Development (H2):
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: update
```

Production (PostgreSQL):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:microservice_db}
    username: ${DB_USERNAME:admin}
    password: ${DB_PASSWORD:secret}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
```

## Docker Support

### Building Docker Image

```bash
docker build -t microservice-template:latest .
```

### Running with Docker Compose

A comprehensive `docker-compose.yml` is provided with the full microservices stack:

| Service | Image | Port | Description |
|---------|-------|------|-------------|
| **microservice-template** | build | 8080 | This application |
| **eureka** | springcloud/eureka | 8761 | Service discovery |
| **config-server** | springcloud/config-server | 8888 | Configuration management |
| **gateway** | springcloud/gateway | 8081 | API gateway routing |
| **postgres** | postgres:14-alpine | 5432 | Database |
| **redis** | redis:7-alpine | 6379 | Cache |
| **rabbitmq** | rabbitmq:3-management-alpine | 5672 / 15672 | Message queue (+ mgmt UI) |
| **kafka** | apache/kafka | 9092 / 9093 | Message streaming (+ JMX) |
| **zookeeper** | confluentinc/cp-zookeeper | 2181 | Kafka coordination |
| **zipkin** | openzipkin/zipkin | 9411 | Distributed tracing UI |
| **prometheus** | prom/prometheus | 9090 | Monitoring |
| **grafana** | grafana/grafana | 3000 | Visualization |

Start all services:
```bash
docker-compose up -d
```

Check running containers:
```bash
docker-compose ps
```

Stop all services:
```bash
docker-compose down
```

### Running Specific Services

To run only the microservice with minimal dependencies:
```bash
docker-compose up -d microservice-template postgres redis
```

To run with message queues:
```bash
docker-compose up -d microservice-template postgres redis rabbitmq kafka zookeeper
```

### Production Docker Compose

Use `docker-compose.prod.yml` for production deployment:
```bash
docker-compose -f docker-compose.prod.yml up -d
```

## API Documentation

OpenAPI documentation is automatically generated and available at:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`
- **OpenAPI YAML**: `http://localhost:8080/v3/api-docs.yaml`

API documentation includes:
- All REST endpoints with request/response schemas
- Authentication requirements (API key, JWT)
- Example requests and responses
- Error response formats (JSON via custom `ErrorController`)

## Testing

### Running Tests

Run all tests:
```bash
mvn test
```

Run specific test class:
```bash
mvn test -Dtest=ApiKeyManagementServiceTest
```

Run integration tests (requires Docker for Testcontainers):
```bash
mvn verify
```

### Test Coverage

- **Unit tests**: Service layer, security components, utilities
- **Integration tests**: Controller endpoints with embedded database
- **Security tests**: Authentication and authorization flows
- **Testcontainers**: Database integration tests with PostgreSQL container

## Monitoring and Observability

### Actuator Endpoints

The following endpoints are exposed (dev profile):

| Endpoint | Description |
|----------|-------------|
| `/manage/health` | Application health (includes database, Redis) |
| `/manage/info` | Application information |
| `/manage/metrics` | Application metrics (JVM, HTTP, cache) |
| `/manage/prometheus` | Prometheus metrics exporter |
| `/manage/loggers` | Log levels configuration |
| `/manage/env` | Environment properties |
| `/manage/beans` | Spring beans |

### Prometheus Metrics

Custom metrics exposed:
- `api_key_validation_total` - API key validation counters
- `http_request_duration_seconds` - HTTP request latency
- `jvm_memory_used` - JVM memory usage
- `database_connections_active` - Database connection pool

### Grafana Dashboards

Pre-configured dashboards in `grafana/` directory:
- Microservice Overview
- API Performance
- Database Metrics
- JVM Monitoring

Access Grafana at `http://localhost:3000` (admin/admin)

### Distributed Tracing

Zipkin distributed tracing configured:
- Access Zipkin UI at `http://localhost:9411`
- Trace all requests across services
- View request latency breakdown

## Deployment

### Kubernetes

Kubernetes manifests are provided in `k8s/` directory:

```bash
kubectl apply -f k8s/
```

Includes:
- Deployment with rolling updates
- Service for internal communication
- ConfigMap for configuration
- Horizontal Pod Autoscaler
- Ingress for external access

### Cloud Deployment

The application is cloud-ready with:
- Externalized configuration
- Health checks for load balancers
- Graceful shutdown
- Stateless design for horizontal scaling
- Graceful degradation when external services are unavailable

## Development Guidelines

### Adding New Features

1. **New REST endpoints**:
   - Add controller in `controller` package
   - Use `@RestController` and `@RequestMapping`
   - Add OpenAPI annotations (`@Operation`, `@ApiResponse`)
   - Implement proper error handling

2. **New services**:
   - Add service class in `service` package
   - Use `@Service` annotation
   - Inject dependencies via constructor
   - Write unit tests with Mockito

3. **New security components**:
   - Extend existing authentication/authorization mechanisms
   - Update `SecurityConfig` for new filters
   - Add audit logging for security events

### Best Practices

- Use constructor injection (Lombok `@RequiredArgsConstructor`)
- Validate inputs with `@Valid` and JSR-303 annotations
- Use DTOs for API requests/responses
- Implement proper error handling with `@RestControllerAdvice`
- Use SLF4J for logging with appropriate levels
- Write unit tests for business logic
- Use profiles for environment-specific configuration
- Follow secure coding practices (OWASP Top 10)

## Troubleshooting

### Common Issues

1. **Application won't start**:
   - Check Java version: `java -version` (must be 17+)
   - Check port availability: `netstat -an | findstr :8080`
   - Check logs: `type logs\microservice-template-dev.log`

2. **Swagger UI returns 500 error**:
   - Most likely caused by Redis being unreachable (rate limiter failure)
   - The `RateLimitService` now degrades gracefully - if this persists,
     check that `RateLimitFilter` excludes `/swagger*` paths
   - Verify the `ErrorController` is returning JSON (not Whitelabel page)

3. **API key authentication failing**:
   - Verify API key format in configuration
   - Check roles assigned to API key
   - Ensure `security.api.validator` is set correctly

4. **Database connection issues**:
   - Verify PostgreSQL is running (if using prod profile)
   - Check connection string in configuration
   - Verify credentials
   - Dev profile uses H2 in-memory - no database needed

5. **Redis connection errors in dev**:
   - **This is expected behavior** in dev without Docker
   - The application will still work - rate limiting degrades gracefully
   - To enable full rate limiting: `docker-compose up -d redis`

6. **Docker Compose issues**:
   - Ensure Docker daemon is running
   - Check port conflicts
   - View logs: `docker-compose logs microservice-template`

### Logs

Logs are written to `logs/` directory and console:
- Development: DEBUG level for detailed debugging
- Production: INFO level with structured JSON format
- Custom `ErrorController` outputs detailed exception info including stack traces

## License

This project is provided under the MIT License.

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

### Areas for Improvement

- Additional authentication providers (OAuth2, SAML)
- More comprehensive API key management UI
- Advanced rate limiting strategies
- Enhanced monitoring dashboards
- Performance benchmarking
- Security penetration testing

## Support

For issues and questions:
- Check the Spring Cloud documentation
- Review existing tests for usage examples
- Create an issue in the repository with detailed description
