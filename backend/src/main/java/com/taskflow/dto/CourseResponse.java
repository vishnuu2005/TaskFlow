package com.taskflow.dto;

public record CourseResponse(
        Long id,
        String name,
        String code,
        String description,
        Long teacherId,
        String teacherName,
        String teacherEmail,
        boolean active,
        long studentCount,
        long assignmentCount,
        String createdAt,
        Boolean isEnrolled
) {}
