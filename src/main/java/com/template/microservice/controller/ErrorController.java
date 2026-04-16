package com.template.microservice.controller;

import com.template.microservice.model.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 自定义错误控制器，替代 Spring Boot 默认的 Whitelabel Error Page。
 * 所有错误以 JSON 格式返回，符合 REST API 规范。
 */
@RestController
@Slf4j
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    private final ErrorAttributes errorAttributes;

    public ErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    @RequestMapping("/error")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleError(WebRequest webRequest) {
        // 包含完整异常信息用于调试
        Map<String, Object> errorDetails = errorAttributes.getErrorAttributes(
                webRequest,
                ErrorAttributeOptions.of(
                    ErrorAttributeOptions.Include.MESSAGE,
                    ErrorAttributeOptions.Include.EXCEPTION,
                    ErrorAttributeOptions.Include.STACK_TRACE
                )
        );

        int status = (int) errorDetails.getOrDefault("status", 500);
        String error = (String) errorDetails.get("error");
        String message = (String) errorDetails.get("message");
        String path = (String) errorDetails.get("path");
        String exception = (String) errorDetails.get("exception");
        String trace = (String) errorDetails.get("trace");

        log.error("Error [{} {}] path={}, exception={}, message={}",
            status, error, path, exception, message);

        ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message(message != null ? message : "An unexpected error occurred")
                .errorCode(error != null ? error : "INTERNAL_SERVER_ERROR")
                .data(Map.of(
                        "status", status,
                        "error", error != null ? error : "Internal Server Error",
                        "path", path != null ? path : "",
                        "exception", exception != null ? exception : "",
                        "trace", trace != null ? trace : "",
                        "timestamp", LocalDateTime.now().toString()
                ))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(status).body(response);
    }
}
