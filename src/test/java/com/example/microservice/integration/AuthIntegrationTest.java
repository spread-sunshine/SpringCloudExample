package com.example.microservice.integration;

import com.example.microservice.model.dto.LoginRequest;
import com.example.microservice.model.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void testRegisterAndLogin() {
        // Register a new user
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("testuser")
                .email("testuser@example.com")
                .password("Test@12345")
                .firstName("Test")
                .lastName("User")
                .build();

        HttpEntity<RegisterRequest> registerEntity = new HttpEntity<>(registerRequest, headers);
        ResponseEntity<Void> registerResponse = restTemplate.exchange(
                getUrl("/api/auth/register"),
                HttpMethod.POST,
                registerEntity,
                Void.class
        );

        assertEquals(HttpStatus.OK, registerResponse.getStatusCode());

        // Login with registered user
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("Test@12345");

        HttpEntity<LoginRequest> loginEntity = new HttpEntity<>(loginRequest, headers);
        ResponseEntity<String> loginResponse = restTemplate.exchange(
                getUrl("/api/auth/login"),
                HttpMethod.POST,
                loginEntity,
                String.class
        );

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());
        assertTrue(loginResponse.getBody().contains("accessToken"));
    }

    @Test
    void testLoginWithInvalidCredentials() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("nonexistent");
        loginRequest.setPassword("wrongpassword");

        HttpEntity<LoginRequest> entity = new HttpEntity<>(loginRequest, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                getUrl("/api/auth/login"),
                HttpMethod.POST,
                entity,
                String.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testAccessProtectedResourceWithoutToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                getUrl("/api/user/profile"),
                String.class
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testRefreshToken() {
        // First register and login to get tokens
        RegisterRequest registerRequest = RegisterRequest.builder()
                .username("refreshuser")
                .email("refresh@example.com")
                .password("Test@12345")
                .firstName("Refresh")
                .lastName("User")
                .build();

        HttpEntity<RegisterRequest> registerEntity = new HttpEntity<>(registerRequest, headers);
        restTemplate.exchange(
                getUrl("/api/auth/register"),
                HttpMethod.POST,
                registerEntity,
                Void.class
        );

        // Login to get tokens
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("refreshuser");
        loginRequest.setPassword("Test@12345");

        HttpEntity<LoginRequest> loginEntity = new HttpEntity<>(loginRequest, headers);
        ResponseEntity<String> loginResponse = restTemplate.exchange(
                getUrl("/api/auth/login"),
                HttpMethod.POST,
                loginEntity,
                String.class
        );

        // Extract refresh token from login response
        String refreshToken = extractRefreshToken(loginResponse.getBody());

        // Use refresh token to get new access token
        String refreshRequest = "{\"refreshToken\":\"" + refreshToken + "\"}";
        HttpEntity<String> refreshEntity = new HttpEntity<>(refreshRequest, headers);
        ResponseEntity<String> refreshResponse = restTemplate.exchange(
                getUrl("/api/auth/refresh"),
                HttpMethod.POST,
                refreshEntity,
                String.class
        );

        assertEquals(HttpStatus.OK, refreshResponse.getStatusCode());
        assertNotNull(refreshResponse.getBody());
        assertTrue(refreshResponse.getBody().contains("accessToken"));
    }

    private String extractRefreshToken(String responseBody) {
        // Simple JSON parsing for test purposes
        // In real tests, use proper JSON parsing
        if (responseBody.contains("refreshToken")) {
            int start = responseBody.indexOf("\"refreshToken\":\"") + 16;
            int end = responseBody.indexOf("\"", start);
            return responseBody.substring(start, end);
        }
        return null;
    }
}