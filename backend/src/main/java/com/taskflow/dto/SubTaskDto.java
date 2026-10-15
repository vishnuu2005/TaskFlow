package com.taskflow.dto;

public record SubTaskDto(
        Long id,
        String title,
        boolean completed
) {}
