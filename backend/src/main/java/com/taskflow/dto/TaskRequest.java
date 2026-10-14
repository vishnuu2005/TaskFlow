
package com.taskflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TaskRequest(

        @NotBlank(message = "Task title is required")
        @Size(
                max = 150,
                message = "Task title must not exceed 150 characters"
        )
        String title,

        @Size(
                max = 1000,
                message = "Description must not exceed 1000 characters"
        )
        String description,

        @Pattern(
                regexp = "LOW|MEDIUM|HIGH",
                message = "Priority must be LOW, MEDIUM, or HIGH"
        )
        String priority,

        LocalDate dueDate

) {
}
