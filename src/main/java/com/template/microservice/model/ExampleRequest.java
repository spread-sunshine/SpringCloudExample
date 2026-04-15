package com.template.microservice.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@Builder
@Schema(description = "Example request model")
public class ExampleRequest {
    @NotBlank(message = "Message cannot be blank")
    @Size(min = 1, max = 100, message = "Message must be between 1 and 100 characters")
    @Schema(description = "Message content", example = "Hello World")
    private String message;
}