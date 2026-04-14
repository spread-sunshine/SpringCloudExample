package com.example.microservice.aspect;

import com.example.microservice.model.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
@Order(1)
public class ApiResponseAspect {

    @Pointcut("execution(* com.example.microservice.controller..*(..)) && " +
              "!execution(* com.example.microservice.exception..*(..))")
    public void controllerMethods() {}

    @Around("controllerMethods()")
    public Object wrapApiResponse(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        log.debug("Wrapping response for {}.{}", className, methodName);
        
        try {
            Object result = joinPoint.proceed();
            
            // If already wrapped or special handling needed
            if (result instanceof ResponseEntity) {
                ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
                Object body = responseEntity.getBody();
                
                if (body instanceof ApiResponse) {
                    return responseEntity;
                }
                
                // Wrap successful responses
                if (responseEntity.getStatusCode().is2xxSuccessful()) {
                    ApiResponse<?> wrappedResponse = ApiResponse.success(body);
                    return ResponseEntity.status(responseEntity.getStatusCode())
                            .headers(responseEntity.getHeaders())
                            .body(wrappedResponse);
                }
                
                // For error responses, exception handler should have wrapped them
                return responseEntity;
            }
            
            // For non-ResponseEntity returns (should be rare)
            return ApiResponse.success(result);
            
        } catch (Exception ex) {
            // Exceptions should be handled by GlobalExceptionHandler
            throw ex;
        }
    }
}