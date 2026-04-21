package com.template.microservice.controller;

import com.template.microservice.model.ExampleResponse;
import com.template.microservice.service.ExampleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
/*
 * Spring Boot 4 Migration:
 * - @MockBean / @SpyBean are REMOVED
 * - Replace with @MockitoBean / @MockitoSpyBean
 *   from org.springframework.test.context.bean.override.mockito
 * - @WebMvcTest no longer auto-provides MockMvc bean,
 *   must add @AutoConfigureMockMvc explicitly.
 */
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExampleController.class)
@AutoConfigureMockMvc
class ExampleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
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