package com.template.microservice.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Example response model")
public class ExampleResponse {
    @Schema(description = "Unique identifier", example = "12345")
    private String id;

    @Schema(description = "Response message", example = "Hello from microservice")
    private String message;

    @Schema(description = "Timestamp in milliseconds", example = "1640995200000")
    private Long timestamp;
}