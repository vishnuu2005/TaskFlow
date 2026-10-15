package com.taskflow.dto;

public record AnnouncementResponse(
        Long id,
        String title,
        String content,
        Long courseId,
        String courseName,
        String courseCode,
        Long authorId,
        String authorName,
        String authorRole,
        String priority,
        String createdAt
) {}
