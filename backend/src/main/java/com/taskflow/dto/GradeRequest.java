package com.taskflow.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GradeRequest(
        @NotNull(message = "Grade is required")
        @DecimalMin(value = "0.0", message = "Grade cannot be negative")
        Double grade,

        @Size(max = 2000, message = "Feedback must not exceed 2000 characters")
        String feedback
) {}
