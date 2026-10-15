package com.taskflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AnnouncementRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title must not exceed 150 characters")
        String title,

        @NotBlank(message = "Content is required")
        @Size(max = 4000, message = "Content must not exceed 4000 characters")
        String content,

        Long courseId,

        @Pattern(regexp = "NORMAL|HIGH|URGENT", message = "Priority must be NORMAL, HIGH, or URGENT")
        String priority
) {}
