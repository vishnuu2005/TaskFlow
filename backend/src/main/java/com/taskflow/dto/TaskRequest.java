package com.taskflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

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

        LocalDate dueDate,

        @Size(max = 50, message = "Category must not exceed 50 characters")
        String category,

        String status,

        List<SubTaskDto> subtasks

) {
    public TaskRequest(String title, String description, String priority, LocalDate dueDate) {
        this(title, description, priority, dueDate, "General", "PENDING", null);
    }
}
