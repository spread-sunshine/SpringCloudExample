# Spring Cloud Microservice Template

A reusable, production-ready template for building Spring Cloud microservices. This template includes common configurations, best practices, and examples to accelerate microservice development.

## Features

- **Spring Boot 2.7.18** with **Spring Cloud 2021.0.8**
- **Service Discovery**: Netflix Eureka client integration
- **Configuration Management**: Spring Cloud Config client (optional)
- **API Gateway**: Spring Cloud Gateway support
- **Circuit Breaker**: Resilience4j integration
- **Distributed Tracing**: Spring Cloud Sleuth
- **Declarative REST Client**: OpenFeign
- **API Documentation**: OpenAPI 3 with Swagger UI
- **Actuator Endpoints**: Health, metrics, and monitoring
- **Docker Support**: Multi-stage Dockerfile and docker-compose
- **Multi-environment Configs**: dev, prod profiles
- **Lombok**: Reduced boilerplate code
- **Validation**: JSR-303 bean validation

## Project Structure

```
src/main/java/com/example/microservice/
├── Application.java              # Main Spring Boot application
├── config/                       # Configuration classes
│   └── FeignConfig.java
├── controller/                   # REST controllers
│   └── ExampleController.java
├── service/                      # Business services
│   └── ExampleService.java
├── model/                        # Data transfer objects
│   ├── ExampleRequest.java
│   └── ExampleResponse.java
└── client/                       # Feign clients
    └── ExampleFeignClient.java
```

## Quick Start

### Prerequisites

- Java 11 or higher
- Maven 3.6+
- Docker and Docker Compose (optional)

### Building and Running

1. **Clone and customize**:
   ```bash
   git clone <repository-url>
   cd spring-cloud-microservice-template
   ```

2. **Update package names**:
   - Replace `com.example.microservice` with your actual package name
   - Update `groupId` and `artifactId` in `pom.xml`

3. **Build the project**:
   ```bash
   mvn clean package
   ```

4. **Run locally**:
   ```bash
   mvn spring-boot:run
   ```
   The application will start on `http://localhost:8080`

5. **Access endpoints**:
   - API: `http://localhost:8080/api/example/{id}`
   - Swagger UI: `http://localhost:8080/swagger-ui.html`
   - Actuator Health: `http://localhost:8080/actuator/health`

## Configuration

### Profiles

- **dev**: Development profile (default)
- **prod**: Production profile

Activate a profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### Key Configuration Files

- `application.yml`: Base configuration
- `application-dev.yml`: Development-specific settings
- `application-prod.yml`: Production-specific settings
- `bootstrap.yml`: Config client bootstrap (optional)

### Service Discovery

To enable Eureka service discovery:

1. Ensure Eureka server is running (included in docker-compose)
2. Set `eureka.client.enabled=true` in your profile
3. Configure Eureka server URL:
   ```yaml
   eureka:
     client:
       service-url:
         defaultZone: http://localhost:8761/eureka/
   ```

## Docker Support

### Building Docker Image

```bash
docker build -t microservice-template:latest .
```

### Running with Docker Compose

A comprehensive `docker-compose.yml` is provided with:

- **Microservice Template** (this application)
- **Eureka Server** (service discovery)
- **Config Server** (configuration management)
- **API Gateway** (routing)
- **PostgreSQL** (database)
- **Redis** (caching)
- **Prometheus** (monitoring)
- **Grafana** (visualization)

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

### Customizing Docker Compose

To run only specific services, comment out unwanted services in `docker-compose.yml` or use:

```bash
docker-compose up -d microservice-template eureka
```

## API Documentation

OpenAPI documentation is automatically generated and available at:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

## Testing

Run unit tests:
```bash
mvn test
```

Run integration tests:
```bash
mvn verify
```

## Monitoring and Observability

### Actuator Endpoints

The following endpoints are exposed (dev profile):

- `/actuator/health` - Application health
- `/actuator/info` - Application info
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics

### Circuit Breaker

Resilience4j circuit breaker is configured. Example configuration:

```yaml
resilience4j.circuitbreaker:
  instances:
    backendA:
      slidingWindowSize: 10
      minimumNumberOfCalls: 5
      failureRateThreshold: 50
```

### Distributed Tracing

Spring Cloud Sleuth adds trace and span IDs to logs. Configure sampling rate:

```yaml
spring:
  sleuth:
    sampler:
      probability: 1.0  # 100% sampling in dev
```

## Development Guidelines

### Adding New Features

1. **New REST endpoints**:
   - Add controller in `controller` package
   - Use `@RestController` and `@RequestMapping`
   - Add OpenAPI annotations for documentation

2. **New services**:
   - Add service class in `service` package
   - Use `@Service` annotation
   - Inject dependencies via constructor

3. **New Feign clients**:
   - Add interface in `client` package
   - Use `@FeignClient` annotation
   - Configure URL in properties

4. **New configuration**:
   - Add configuration class in `config` package
   - Use `@Configuration` annotation
   - Define beans as needed

### Best Practices

- Use constructor injection (Lombok `@RequiredArgsConstructor`)
- Validate inputs with `@Valid` and JSR-303 annotations
- Use DTOs for API requests/responses
- Implement proper error handling with `@ControllerAdvice`
- Use SLF4J for logging with appropriate levels
- Write unit tests for business logic
- Use profiles for environment-specific configuration

## Common Tasks

### Changing Port

Update `server.port` in the appropriate profile YAML file.

### Disabling Features

Remove or comment out dependencies in `pom.xml` and corresponding configurations.

### Adding Database Support

1. Add Spring Data JPA dependency:
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-data-jpa</artifactId>
   </dependency>
   ```

2. Configure datasource in profile YAML.

### Customizing Logging

Update `logging.level` in profile YAML files.

## Troubleshooting

### Application Won't Start

1. Check Java version: `java -version`
2. Check port availability: `netstat -an | findstr :8080`
3. Check logs: `tail -f logs/application.log`

### Eureka Registration Issues

1. Verify Eureka server is running
2. Check `eureka.client.service-url.defaultZone` configuration
3. Check network connectivity between services

### Configuration Not Loading

1. Verify profile is active
2. Check `spring.config.import` or `spring.cloud.config.uri`
3. Check YAML syntax

## License

This template is provided under the MIT License.

## Contributing

Feel free to customize this template for your specific needs. Suggested improvements:

- Add more example implementations
- Include database migration scripts
- Add Kubernetes deployment manifests
- Include more comprehensive tests
- Add security configurations (Spring Security, OAuth2)

## Support

For issues and questions, please check the Spring Cloud documentation or create an issue in the repository.