package com.taskflow.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TaskResponse(
    Long id,
    String title,
    String description,
    boolean completed,
    String priority,
    LocalDate dueDate,
    String category,
    String status,
    LocalDateTime createdAt,
    List<SubTaskDto> subtasks
) {
    public TaskResponse(
            Long id,
            String title,
            String description,
            boolean completed,
            String priority,
            LocalDate dueDate
    ) {
        this(id, title, description, completed, priority, dueDate, "General", completed ? "COMPLETED" : "PENDING", null, List.of());
    }
}
