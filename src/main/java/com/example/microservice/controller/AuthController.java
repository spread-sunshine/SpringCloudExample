package com.example.microservice.controller;

import com.example.microservice.model.dto.LoginRequest;
import com.example.microservice.model.dto.LoginResponse;
import com.example.microservice.model.dto.RefreshTokenRequest;
import com.example.microservice.model.dto.RegisterRequest;
import com.example.microservice.security.JwtTokenProvider;
import com.example.microservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "User login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtTokenProvider.createToken(
                loginRequest.getUsername(),
                authentication.getAuthorities()
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(loginRequest.getUsername());

        return ResponseEntity.ok(LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .username(loginRequest.getUsername())
                .authorities(authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toArray(String[]::new))
                .build());
    }

    @PostMapping("/register")
    @Operation(summary = "User registration")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest registerRequest) {
        authService.register(registerRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        String username = jwtTokenProvider.getUsernameFromToken(refreshTokenRequest.getRefreshToken());

        // Validate refresh token
        if (!jwtTokenProvider.validateToken(refreshTokenRequest.getRefreshToken())) {
            throw new RuntimeException("Invalid refresh token");
        }

        // Get user details and create new tokens
        Authentication authentication = jwtTokenProvider.getAuthentication(refreshTokenRequest.getRefreshToken());
        String newAccessToken = jwtTokenProvider.createToken(username, authentication.getAuthorities());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(username);

        return ResponseEntity.ok(LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .username(username)
                .authorities(authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toArray(String[]::new))
                .build());
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout")
    public ResponseEntity<Void> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
    }
}