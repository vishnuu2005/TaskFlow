package com.taskflow.dto;

import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        String currentPassword,

        @Size(min = 8, max = 72, message = "New password must be between 8 and 72 characters")
        String newPassword
) {}
