package com.taskflow.dto;

import jakarta.validation.constraints.Email;

public record EnrollmentRequest(
        Long studentId,
        @Email(message = "Enter a valid student email")
        String studentEmail
) {}
