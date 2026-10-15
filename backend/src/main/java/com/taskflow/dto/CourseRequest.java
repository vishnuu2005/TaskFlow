package com.taskflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourseRequest(
        @NotBlank(message = "Course name is required")
        @Size(max = 100, message = "Course name must not exceed 100 characters")
        String name,

        @NotBlank(message = "Course code is required")
        @Size(max = 30, message = "Course code must not exceed 30 characters")
        String code,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        Long teacherId
) {}
