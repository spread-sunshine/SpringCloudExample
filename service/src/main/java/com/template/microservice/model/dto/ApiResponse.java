package com.template.microservice.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response")
public class ApiResponse<T> {
    @Schema(description = "Whether the request was successful", example = "true")
    private boolean success;

    @Schema(description = "Response message", example = "Operation successful")
    private String message;

    @Schema(description = "Error code if any", example = "SUCCESS")
    private String errorCode;

    @Schema(description = "Response data")
    private T data;

    @Schema(description = "Response timestamp", example = "2023-12-01T10:30:00")
    private LocalDateTime timestamp;

    @Schema(description = "API version", example = "1.0")
    private String version;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Success")
                .errorCode("SUCCESS")
                .data(data)
                .timestamp(LocalDateTime.now())
                .version("1.0")
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .errorCode("SUCCESS")
                .data(data)
                .timestamp(LocalDateTime.now())
                .version("1.0")
                .build();
    }

    public static ApiResponse<Void> error(String message, String errorCode) {
        return ApiResponse.<Void>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .timestamp(LocalDateTime.now())
                .version("1.0")
                .build();
    }
}