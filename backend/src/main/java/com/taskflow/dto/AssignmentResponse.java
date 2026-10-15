package com.taskflow.dto;

import java.time.LocalDate;

public record AssignmentResponse(
        Long id,
        String title,
        String instructions,
        LocalDate dueDate,
        String priority,
        Integer points,
        String attachmentUrl,
        Long courseId,
        String courseName,
        String courseCode,
        String teacherName,
        String submissionStatus,
        Double myGrade,
        String myFeedback,
        String mySubmissionNotes,
        String submittedAt,
        Long totalSubmissions,
        Long gradedSubmissions
) {}
