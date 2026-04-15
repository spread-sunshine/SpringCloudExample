# Spring Cloud Microservice Template

A reusable template for Spring Cloud microservices with comprehensive security including API key authentication, JWT, role-based access control, and full observability stack.

## Features

- **Spring Boot 3.2.5** with **Spring Cloud 2023.0.3**
- **Dual Authentication**: API Key + JWT token support
- **Role-Based Access Control**: Fine-grained authorization with Spring Security
- **API Key Management**: Full lifecycle management (creation, validation, rotation, revocation)
- **Security**: Spring Security with OAuth2 resource server, CORS, CSRF protection, security headers
- **Service Discovery**: Netflix Eureka client integration
- **Configuration Management**: Spring Cloud Config client support
- **API Gateway**: Spring Cloud Gateway support
- **Circuit Breaker**: Resilience4j with bulkhead, retry, rate limiting
- **Distributed Tracing**: Micrometer Tracing with Zipkin integration
- **Declarative REST Client**: OpenFeign
- **API Documentation**: OpenAPI 3 with Swagger UI
- **Actuator Endpoints**: Health, metrics, info, Prometheus exporter
- **Database Support**: PostgreSQL (production), H2 (development), Spring Data JPA, Flyway migrations
- **Caching**: Redis with Spring Cache abstraction
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
│   ├── ExampleController.java        # Example REST endpoints
│   └── AuthController.java           # Authentication endpoints
├── service/                          # Business services
│   ├── ExampleService.java           # Example business logic
│   ├── AuthService.java              # Authentication service
│   ├── RateLimitService.java         # Rate limiting service
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
│   └── RateLimitFilter.java          # Rate limiting filter
├── aspect/                           # AOP aspects
│   └── ApiResponseAspect.java        # Standardized API response wrapping
├── exception/                        # Exception handling
│   ├── GlobalExceptionHandler.java   # Global exception handler
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
   The application will start on `http://localhost:8080`

4. **Access endpoints**:
   - API: `http://localhost:8080/api/example/{id}`
   - Authentication: `http://localhost:8080/api/auth/login`
   - Swagger UI: `http://localhost:8080/swagger-ui.html`
   - Actuator Health: `http://localhost:8080/actuator/health`
   - Prometheus Metrics: `http://localhost:8080/actuator/prometheus`

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

- **dev**: Development profile (default) - uses H2 in-memory database, debug logging
- **prod**: Production profile - uses PostgreSQL, optimized settings

Activate a profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Key Configuration Files

- `application.yml`: Base configuration
- `application-dev.yml`: Development-specific settings
- `application-prod.yml`: Production-specific settings
- `application-test.yml`: Test-specific settings

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

- **Microservice** (this application)
- **Eureka Server** (service discovery)
- **Config Server** (configuration management)
- **API Gateway** (routing)
- **PostgreSQL** (database)
- **Redis** (caching)
- **RabbitMQ** (message queue)
- **Kafka** (message streaming)
- **Prometheus** (monitoring)
- **Grafana** (visualization)
- **Zipkin** (distributed tracing)

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

To run only the microservice with dependencies:
```bash
docker-compose up -d microservice-template postgres redis
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
- Error response formats

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

- `/actuator/health` - Application health (includes database, Redis)
- `/actuator/info` - Application information
- `/actuator/metrics` - Application metrics (JVM, HTTP, cache)
- `/actuator/prometheus` - Prometheus metrics exporter
- `/actuator/loggers` - Log levels configuration
- `/actuator/env` - Environment properties
- `/actuator/beans` - Spring beans

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
- Implement proper error handling with `@ControllerAdvice`
- Use SLF4J for logging with appropriate levels
- Write unit tests for business logic
- Use profiles for environment-specific configuration
- Follow secure coding practices (OWASP Top 10)

## Troubleshooting

### Common Issues

1. **Application won't start**:
   - Check Java version: `java -version` (must be 17+)
   - Check port availability: `netstat -an | findstr :8080`
   - Check logs: `tail -f logs/microservice-template-dev.log`

2. **API key authentication failing**:
   - Verify API key format in configuration
   - Check roles assigned to API key
   - Ensure `security.api.validator` is set correctly

3. **Database connection issues**:
   - Verify PostgreSQL is running (if using prod profile)
   - Check connection string in configuration
   - Verify credentials

4. **Docker Compose issues**:
   - Ensure Docker daemon is running
   - Check port conflicts
   - View logs: `docker-compose logs microservice-template`

### Logs

Logs are written to `logs/` directory and console:
- Development: DEBUG level for detailed debugging
- Production: INFO level with structured JSON format

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