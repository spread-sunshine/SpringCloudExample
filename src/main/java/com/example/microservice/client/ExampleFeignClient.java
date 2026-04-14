package com.example.microservice.client;

import com.example.microservice.model.ExampleResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "example-service", url = "${feign.client.example-service.url:}")
public interface ExampleFeignClient {

    @GetMapping("/api/example/{id}")
    ExampleResponse getExample(@PathVariable("id") String id);
}