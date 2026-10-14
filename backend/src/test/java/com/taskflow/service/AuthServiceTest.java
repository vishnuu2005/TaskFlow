package com.taskflow.service;

import com.taskflow.dto.LoginRequest;
import com.taskflow.dto.RegisterRequest;
import com.taskflow.entity.User;
import com.taskflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder);
    }

    @Test
    void register_ShouldSucceedWhenValid() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane@example.com", "Password123!");

        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed-pass");

        User savedUser = new User();
        savedUser.setName("Jane Doe");
        savedUser.setEmail("jane@example.com");
        savedUser.setPassword("hashed-pass");

        // Use reflection or subclass if id has no setter; User entity has no setId, let's verify
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            return u;
        });

        assertDoesNotThrow(() -> authService.register(request));
        verify(userRepository).saveAndFlush(any(User.class));
    }

    @Test
    void register_ShouldThrowWhenDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane@example.com", "Password123!");
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> authService.register(request));
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void register_ShouldThrowWhenPasswordTooShort() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane@example.com", "short");

        assertThrows(ResponseStatusException.class, () -> authService.register(request));
    }

    @Test
    void login_ShouldSucceedWhenCredentialsMatch() {
        LoginRequest request = new LoginRequest("jane@example.com", "Password123!");

        User user = new User();
        user.setEmail("jane@example.com");
        user.setPassword("hashed-pass");

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hashed-pass")).thenReturn(true);

        User result = authService.login(request);
        assertNotNull(result);
        assertEquals("jane@example.com", result.getEmail());
    }

    @Test
    void login_ShouldThrowBadCredentialsWhenPasswordMismatch() {
        LoginRequest request = new LoginRequest("jane@example.com", "WrongPassword!");

        User user = new User();
        user.setEmail("jane@example.com");
        user.setPassword("hashed-pass");

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword!", "hashed-pass")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_ShouldThrowBadCredentialsWhenUserNotFound() {
        LoginRequest request = new LoginRequest("unknown@example.com", "Password123!");
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }
}
