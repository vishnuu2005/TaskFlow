
package com.taskflow.dto;

import java.time.LocalDate;

public record TaskResponse(
    Long id,
    String title,
    String description,
    boolean completed,
    String priority,
    LocalDate dueDate
) {}
