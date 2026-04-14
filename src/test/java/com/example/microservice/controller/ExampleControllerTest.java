package com.example.microservice.controller;

import com.example.microservice.model.ExampleResponse;
import com.example.microservice.service.ExampleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExampleController.class)
class ExampleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExampleService exampleService;

    @Test
    void getExample_ShouldReturnExample() throws Exception {
        ExampleResponse response = ExampleResponse.builder()
                .id("123")
                .message("Hello")
                .timestamp(1640995200000L)
                .build();

        when(exampleService.getExample(anyString())).thenReturn(response);

        mockMvc.perform(get("/api/example/123"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("123"))
                .andExpect(jsonPath("$.message").value("Hello"))
                .andExpect(jsonPath("$.timestamp").value(1640995200000L));
    }

    @Test
    void health_ShouldReturnHealthy() throws Exception {
        mockMvc.perform(get("/api/example/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Service is healthy"));
    }
}