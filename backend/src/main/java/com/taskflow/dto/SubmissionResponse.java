package com.taskflow.dto;

public record SubmissionResponse(
        Long id,
        Long assignmentId,
        String assignmentTitle,
        Integer assignmentPoints,
        Long studentId,
        String studentName,
        String studentEmail,
        String submissionNotes,
        String attachmentUrl,
        String status,
        String submittedAt,
        Double grade,
        String feedback,
        String feedbackAt
) {}
