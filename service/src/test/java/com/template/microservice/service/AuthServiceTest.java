package com.template.microservice.service;

import com.template.microservice.model.dto.RegisterRequest;
import com.template.microservice.model.entity.Role;
import com.template.microservice.model.entity.User;
import com.template.microservice.repository.RoleRepository;
import com.template.microservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private RegisterRequest validRegisterRequest;
    private User existingUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        validRegisterRequest = RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .firstName("Test")
                .lastName("User")
                .build();

        existingUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .locked(false)
                .build();

        userRole = Role.builder()
                .id(1L)
                .name("ROLE_USER")
                .description("Default user role")
                .build();
    }

    @Test
    void register_ShouldSaveUser_WhenUsernameAndEmailAreAvailable() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // Act
        authService.register(validRegisterRequest);

        // Assert
        verify(userRepository, times(1)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        
        assertEquals(validRegisterRequest.getUsername(), savedUser.getUsername());
        assertEquals(validRegisterRequest.getEmail(), savedUser.getEmail());
        assertEquals("encodedPassword", savedUser.getPassword());
        assertEquals(validRegisterRequest.getFirstName(), savedUser.getFirstName());
        assertEquals(validRegisterRequest.getLastName(), savedUser.getLastName());
        assertTrue(savedUser.isEnabled());
        assertFalse(savedUser.isLocked());
        assertTrue(savedUser.getRoles().contains(userRole));
    }

    @Test
    void register_ShouldCreateRole_WhenRoleDoesNotExist() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenReturn(userRole);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // Act
        authService.register(validRegisterRequest);

        // Assert
        verify(roleRepository, times(1)).save(any(Role.class));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_ShouldThrowException_WhenUsernameAlreadyExists() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.register(validRegisterRequest));
        assertEquals("Username is already taken", exception.getMessage());
        
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_ShouldThrowException_WhenEmailAlreadyExists() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.register(validRegisterRequest));
        assertEquals("Email is already in use", exception.getMessage());
        
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateLastLogin_ShouldUpdateTimestamp_WhenUserExists() {
        // Arrange
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // Act
        authService.updateLastLogin("testuser");

        // Assert
        verify(userRepository, times(1)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertNotNull(savedUser.getLastLoginAt());
    }

    @Test
    void updateLastLogin_ShouldDoNothing_WhenUserDoesNotExist() {
        // Arrange
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        // Act
        authService.updateLastLogin("nonexistent");

        // Assert
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void validateUserCredentials_ShouldReturnTrue_WhenCredentialsAreValid() {
        // Arrange
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        // Act
        boolean isValid = authService.validateUserCredentials("testuser", "password123");

        // Assert
        assertTrue(isValid);
    }

    @Test
    void validateUserCredentials_ShouldReturnFalse_WhenUserDoesNotExist() {
        // Arrange
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        // Act
        boolean isValid = authService.validateUserCredentials("nonexistent", "password123");

        // Assert
        assertFalse(isValid);
    }

    @Test
    void validateUserCredentials_ShouldReturnFalse_WhenPasswordDoesNotMatch() {
        // Arrange
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // Act
        boolean isValid = authService.validateUserCredentials("testuser", "wrongpassword");

        // Assert
        assertFalse(isValid);
    }

    @Test
    void register_ShouldEncodePassword() {
        // Arrange
        String rawPassword = "password123";
        String encodedPassword = "$2a$10$encodedPasswordHash";
        
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // Act
        authService.register(validRegisterRequest);

        // Assert
        verify(passwordEncoder, times(1)).encode(rawPassword);
        verify(userRepository, times(1)).save(userCaptor.capture());
        assertEquals(encodedPassword, userCaptor.getValue().getPassword());
    }
}