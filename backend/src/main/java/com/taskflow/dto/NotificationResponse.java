package com.taskflow.dto;

public record NotificationResponse(
        Long id,
        String title,
        String message,
        String type,
        boolean isRead,
        String link,
        String createdAt
) {}
