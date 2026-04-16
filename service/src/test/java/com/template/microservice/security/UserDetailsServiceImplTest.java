package com.template.microservice.security;

import com.template.microservice.model.entity.Role;
import com.template.microservice.model.entity.User;
import com.template.microservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User testUser;
    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        // Create roles
        userRole = Role.builder()
                .id(1L)
                .name("ROLE_USER")
                .description("Regular user role")
                .build();
        
        adminRole = Role.builder()
                .id(2L)
                .name("ROLE_ADMIN")
                .description("Administrator role")
                .build();
        
        // Create user with roles
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        roles.add(adminRole);
        
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword123")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .locked(false)
                .roles(roles)
                .build();
    }

    @Test
    void loadUserByUsername_ShouldReturnUserDetails_WhenUserExists() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("encodedPassword123", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        
        // Verify authorities
        var authorities = userDetails.getAuthorities();
        assertEquals(2, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_USER")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        
        // Verify repository was called
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    void loadUserByUsername_ShouldThrowUsernameNotFoundException_WhenUserNotFound() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername("nonexistent");
        });
        
        assertEquals("User not found with username: nonexistent", exception.getMessage());
        verify(userRepository, times(1)).findByUsername("nonexistent");
    }

    @Test
    void loadUserByUsername_ShouldHandleDisabledUser() {
        // Arrange
        testUser.setEnabled(false);
        when(userRepository.findByUsername("disableduser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("disableduser");

        // Assert
        assertNotNull(userDetails);
        assertFalse(userDetails.isEnabled());
        assertEquals("testuser", userDetails.getUsername());
        
        verify(userRepository, times(1)).findByUsername("disableduser");
    }

    @Test
    void loadUserByUsername_ShouldHandleLockedUser() {
        // Arrange
        testUser.setLocked(true);
        when(userRepository.findByUsername("lockeduser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("lockeduser");

        // Assert
        assertNotNull(userDetails);
        assertFalse(userDetails.isAccountNonLocked());
        assertEquals("testuser", userDetails.getUsername());
        
        verify(userRepository, times(1)).findByUsername("lockeduser");
    }

    @Test
    void loadUserByUsername_ShouldHandleUserWithNoRoles() {
        // Arrange
        testUser.setRoles(new HashSet<>()); // Empty roles
        when(userRepository.findByUsername("noroles")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("noroles");

        // Assert
        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().isEmpty());
        
        verify(userRepository, times(1)).findByUsername("noroles");
    }

    @Test
    void loadUserByUsername_ShouldHandleUserWithSingleRole() {
        // Arrange
        Set<Role> singleRole = new HashSet<>();
        singleRole.add(userRole);
        testUser.setRoles(singleRole);
        
        when(userRepository.findByUsername("singlerole")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("singlerole");

        // Assert
        assertNotNull(userDetails);
        var authorities = userDetails.getAuthorities();
        assertEquals(1, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_USER")));
        
        verify(userRepository, times(1)).findByUsername("singlerole");
    }

    @Test
    void loadUserByUsername_ShouldHandleCaseSensitiveUsername() {
        // Arrange
        when(userRepository.findByUsername("TestUser")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act & Assert - Different case should be treated as different username
        assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername("TestUser");
        });
        
        // Original case should work
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");
        assertNotNull(userDetails);
        
        verify(userRepository, times(1)).findByUsername("TestUser");
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    void loadUserByUsername_ShouldHandleNullUsername() {
        // Arrange
        String nullUsername = null;

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userDetailsService.loadUserByUsername(nullUsername);
        });
    }

    @Test
    void loadUserByUsername_ShouldHandleEmptyUsername() {
        // Arrange
        String emptyUsername = "";

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername(emptyUsername);
        });
        
        verify(userRepository, times(1)).findByUsername("");
    }

    @Test
    void loadUserByUsername_ShouldHandleWhitespaceUsername() {
        // Arrange
        String whitespaceUsername = "   ";
        when(userRepository.findByUsername("   ")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername(whitespaceUsername);
        });
        
        verify(userRepository, times(1)).findByUsername("   ");
    }

    @Test
    void loadUserByUsername_ShouldMapRoleNamesCorrectly() {
        // Arrange
        Set<Role> customRoles = new HashSet<>();
        customRoles.add(Role.builder().name("CUSTOM_ROLE_1").build());
        customRoles.add(Role.builder().name("CUSTOM_ROLE_2").build());
        customRoles.add(Role.builder().name("ADMIN").build());
        
        testUser.setRoles(customRoles);
        when(userRepository.findByUsername("customroles")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("customroles");

        // Assert
        var authorities = userDetails.getAuthorities();
        assertEquals(3, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("CUSTOM_ROLE_1")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("CUSTOM_ROLE_2")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ADMIN")));
        
        verify(userRepository, times(1)).findByUsername("customroles");
    }

    @Test
    void loadUserByUsername_ShouldCacheRepositoryCall() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act - Call multiple times
        UserDetails userDetails1 = userDetailsService.loadUserByUsername("testuser");
        UserDetails userDetails2 = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(userDetails1);
        assertNotNull(userDetails2);
        assertEquals(userDetails1.getUsername(), userDetails2.getUsername());
        
        // Repository should be called twice (no caching in service layer)
        verify(userRepository, times(2)).findByUsername("testuser");
    }

    @Test
    void loadUserByUsername_ShouldHandleDatabaseException() {
        // Arrange
        when(userRepository.findByUsername("testuser"))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            userDetailsService.loadUserByUsername("testuser");
        });
        
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    void loadUserByUsername_ShouldReturnCorrectAccountStatusFlags() {
        // Arrange
        testUser.setEnabled(true);
        testUser.setLocked(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        
        // Test with locked account
        testUser.setLocked(true);
        when(userRepository.findByUsername("locked")).thenReturn(Optional.of(testUser));
        
        UserDetails lockedUserDetails = userDetailsService.loadUserByUsername("locked");
        assertFalse(lockedUserDetails.isAccountNonLocked());
    }

    @Test
    void loadUserByUsername_ShouldHandleSpecialCharactersInUsername() {
        // Arrange
        String specialUsername = "test.user@domain";
        User specialUser = User.builder()
                .username(specialUsername)
                .password("password123")
                .enabled(true)
                .locked(false)
                .roles(new HashSet<>())
                .build();
        
        when(userRepository.findByUsername(specialUsername)).thenReturn(Optional.of(specialUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(specialUsername);

        // Assert
        assertNotNull(userDetails);
        assertEquals(specialUsername, userDetails.getUsername());
        
        verify(userRepository, times(1)).findByUsername(specialUsername);
    }
}