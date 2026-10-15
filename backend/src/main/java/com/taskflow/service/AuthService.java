
package com.taskflow.service;

import com.taskflow.dto.RegisterRequest;
import com.taskflow.dto.LoginRequest;
import com.taskflow.entity.Role;
import com.taskflow.entity.User;
import com.taskflow.repository.UserRepository;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ==========================================
    // USER REGISTRATION
    // ==========================================

    @Transactional
    public Long register(RegisterRequest request) {

        // Step 1: Validate registration details
        if (request == null
                || request.name() == null
                || request.name().isBlank()
                || request.name().length() > 100
                || request.email() == null
                || request.email().isBlank()
                || request.email().length() > 150
                || request.password() == null
                || request.password().length() < 8
                || request.password().getBytes(
                        StandardCharsets.UTF_8).length > 72) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid registration details"
            );
        }

        // Step 2: Normalize email
        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Step 3: Validate email format
        if (!email.matches(
                "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid email address"
            );
        }

        // Step 4: Check duplicate email
        if (userRepository.existsByEmail(email)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered"
            );
        }

        // Step 5: Determine Role (ADMIN cannot be self-registered)
        Role userRole = Role.STUDENT;
        if (request.role() != null && !request.role().isBlank()) {
            String roleUpper = request.role().trim().toUpperCase(Locale.ROOT);
            if ("ADMIN".equals(roleUpper)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Public registration as ADMIN is not permitted"
                );
            }
            try {
                userRole = Role.valueOf(roleUpper);
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid role: " + request.role() + ". Must be STUDENT or TEACHER"
                );
            }
        }

        // Step 6: Create user
        User user = new User();

        user.setName(request.name().trim());
        user.setEmail(email);
        user.setRole(userRole);
        user.setActive(true);

        // Step 7: Hash password using BCrypt
        String hashedPassword =
                passwordEncoder.encode(request.password());

        user.setPassword(hashedPassword);

        // Step 8: Save user in MySQL/H2
        try {

            User savedUser =
                    userRepository.saveAndFlush(user);

            return savedUser.getId();

        } catch (DataIntegrityViolationException ex) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Registration could not be completed",
                    ex
            );
        }
    }

    // ==========================================
    // USER LOGIN
    // ==========================================

    @Transactional(readOnly = true)
    public User login(LoginRequest request) {

        // Step 1: Validate login details
        if (request == null
                || request.email() == null
                || request.email().isBlank()
                || request.password() == null
                || request.password().isBlank()) {

            throw new BadCredentialsException(
                    "Invalid email or password"
            );
        }

        // Step 2: Normalize email
        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Step 3: Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Invalid email or password"
                        )
                );

        // Step 4: Check if account is active
        if (!user.isActive()) {
            throw new BadCredentialsException(
                    "Account is deactivated. Please contact an administrator."
            );
        }

        // Step 5: Verify password using BCrypt
        boolean passwordMatches =
                passwordEncoder.matches(
                        request.password(),
                        user.getPassword()
                );

        // Step 6: Reject incorrect password
        if (!passwordMatches) {

            throw new BadCredentialsException(
                    "Invalid email or password"
            );
        }

        // Step 7: Login successful
        // This user will be used for JWT generation
        return user;
    }
}
