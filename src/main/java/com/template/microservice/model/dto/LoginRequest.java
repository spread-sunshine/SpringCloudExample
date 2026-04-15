package com.template.microservice.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
@Schema(description = "Login request")
public class LoginRequest {
    @NotBlank(message = "Username cannot be blank")
    @Schema(description = "Username", example = "admin", required = true)
    private String username;

    @NotBlank(message = "Password cannot be blank")
    @Schema(description = "Password", example = "password123", required = true)
    private String password;
}