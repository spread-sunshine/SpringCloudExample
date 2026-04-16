package com.template.microservice.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT Authorization header using the Bearer scheme. Example: 'Bearer {token}'"
)
@SecurityScheme(
        name = "apiKey",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-API-Key",
        description = "API Key for external services"
)
@SecurityScheme(
        name = "basicAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "basic",
        description = "Basic authentication"
)
public class SwaggerConfig {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String REQUEST_ID_DESCRIPTION = "Request ID for tracing";
    private static final String ACCEPT_LANGUAGE_HEADER = "Accept-Language";
    private static final String ACCEPT_LANGUAGE_DESCRIPTION = "Language preference";
    private static final String ACCEPT_LANGUAGE_EXAMPLE = "en-US";

    private java.util.List<io.swagger.v3.oas.models.Operation> getAllOperations(io.swagger.v3.oas.models.PathItem pathItem) {
        return java.util.Arrays.asList(
                pathItem.getGet(),
                pathItem.getPost(),
                pathItem.getPut(),
                pathItem.getDelete(),
                pathItem.getPatch()
        );
    }

    @Bean
    public OpenApiCustomizer openApiCustomiser() {
        return openApi -> {
            // Add global parameters
            openApi.getPaths().values().forEach(pathItem -> {
                addGlobalParameters(pathItem);
            });

            // Add common responses
            openApi.getPaths().forEach((path, pathItem) -> {
                addCommonResponses(pathItem);
            });

            // Add security examples
            addSecurityExamples(openApi);

            // Add error response examples
            addErrorResponseExamples(openApi);
        };
    }

    private void addGlobalParameters(PathItem pathItem) {
        getAllOperations(pathItem).forEach(operation -> {
            if (operation != null) {
                // Add common parameters
                operation.addParametersItem(new Parameter()
                        .name(REQUEST_ID_HEADER)
                        .in("header")
                        .description(REQUEST_ID_DESCRIPTION)
                        .required(false)
                        .schema(new Schema<String>().type("string")));

                operation.addParametersItem(new Parameter()
                        .name(ACCEPT_LANGUAGE_HEADER)
                        .in("header")
                        .description(ACCEPT_LANGUAGE_DESCRIPTION)
                        .required(false)
                        .schema(new Schema<String>().type("string").example(ACCEPT_LANGUAGE_EXAMPLE)));
            }
        });
    }

    private void addCommonResponses(PathItem pathItem) {
        getAllOperations(pathItem).forEach(operation -> {
            if (operation != null) {
                addCommonResponseToOperation(operation);
            }
        });
    }

    private void addCommonResponseToOperation(Operation operation) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }

        addResponseIfMissing(responses, "401", "Unauthorized - Invalid or missing authentication token");
        addResponseIfMissing(responses, "403", "Forbidden - Insufficient permissions");
        addResponseIfMissing(responses, "429", "Too Many Requests - Rate limit exceeded");
        addResponseIfMissing(responses, "500", "Internal Server Error");
    }

    private void addResponseIfMissing(ApiResponses responses, String statusCode, String description) {
        if (!responses.containsKey(statusCode)) {
            responses.addApiResponse(statusCode, new ApiResponse()
                    .description(description)
                    .content(new Content().addMediaType("application/json",
                            new MediaType().schema(new Schema<Map<String, Object>>()
                                    .$ref("#/components/schemas/ApiResponse")))));
        }
    }

    private void addSecurityExamples(OpenAPI openApi) {
        // Add security scheme examples
        if (openApi.getComponents() == null) {
            openApi.setComponents(new io.swagger.v3.oas.models.Components());
        }

        // Add example for JWT token
        Map<String, io.swagger.v3.oas.models.examples.Example> jwtExamples = new HashMap<>();
        jwtExamples.put("validJWT", new Example()
                .summary("Valid JWT Token")
                .description("Example of a valid JWT token")
                .value("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"));

        io.swagger.v3.oas.models.examples.Example example = new io.swagger.v3.oas.models.examples.Example();
        // example.setExamples(jwtExamples); // Method not available in this version
        openApi.getComponents().addExamples("JWTExamples", example);
    }

    private void addErrorResponseExamples(OpenAPI openApi) {
        // Add error response examples
        if (openApi.getComponents() == null) {
            openApi.setComponents(new io.swagger.v3.oas.models.Components());
        }

        // Define error response schema
        Schema<?> errorSchema = new Schema<Map<String, Object>>()
                .type("object")
                .addProperty("success", new Schema<Boolean>().type("boolean").example(false))
                .addProperty("message", new Schema<String>().type("string").example("Error message"))
                .addProperty("errorCode", new Schema<String>().type("string").example("VALIDATION_ERROR"))
                .addProperty("timestamp", new Schema<String>().type("string").example("2023-12-01T10:30:00"))
                .addProperty("version", new Schema<String>().type("string").example("1.0"));

        openApi.getComponents().addSchemas("ApiResponse", errorSchema);

        // Add external documentation
        openApi.setExternalDocs(new ExternalDocumentation()
                .description("Microservice Template Documentation")
                .url("https://docs.example.com/microservice-template"));
    }

    @Bean
    public OpenApiCustomizer operationCustomiser() {
        return openApi -> openApi.getPaths().forEach((path, pathItem) -> {
            pathItem.readOperations().forEach(operation -> {
                // Add operation ID if not present
                if (operation.getOperationId() == null) {
                    String operationId = generateOperationId(path, operation);
                    operation.setOperationId(operationId);
                }

                // Add tags based on path
                if (operation.getTags() == null || operation.getTags().isEmpty()) {
                    String tag = extractTagFromPath(path);
                    operation.addTagsItem(tag);
                }

                // Add security requirements
                if (operation.getSecurity() == null || operation.getSecurity().isEmpty()) {
                    if (!path.contains("/auth/") && !path.contains("/public/")) {
                        operation.addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement()
                                .addList("bearerAuth"));
                    }
                }
            });
        });
    }

    private String generateOperationId(String path, Operation operation) {
        String method = operation.getOperationId() != null ? "" : operation.getClass().getSimpleName();
        String cleanedPath = path.replaceAll("[{}]", "")
                .replaceAll("/", "_")
                .replaceAll("-", "_");
        return method.toLowerCase() + cleanedPath;
    }

    private String extractTagFromPath(String path) {
        if (path.contains("/auth/")) {
            return "Authentication";
        } else if (path.contains("/admin/")) {
            return "Admin";
        } else if (path.contains("/user/")) {
            return "User";
        } else if (path.contains("/public/")) {
            return "Public";
        }
        return "General";
    }
}