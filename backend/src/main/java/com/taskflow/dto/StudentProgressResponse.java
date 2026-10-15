package com.taskflow.dto;

public record StudentProgressResponse(
        Long studentId,
        String studentName,
        String studentEmail,
        String enrolledAt,
        long completedAssignments,
        long totalAssignments,
        Double averageGrade
) {}
