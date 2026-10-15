package com.taskflow.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AssignmentRequest(
        @NotBlank(message = "Assignment title is required")
        @Size(max = 150, message = "Title must not exceed 150 characters")
        String title,

        @Size(max = 3000, message = "Instructions must not exceed 3000 characters")
        String instructions,

        LocalDate dueDate,

        @Pattern(regexp = "LOW|MEDIUM|HIGH", message = "Priority must be LOW, MEDIUM, or HIGH")
        String priority,

        @Min(value = 1, message = "Points must be at least 1")
        Integer points,

        @Size(max = 500, message = "Attachment URL too long")
        String attachmentUrl
) {}
