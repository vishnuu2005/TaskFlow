package com.taskflow.dto;

import jakarta.validation.constraints.Size;

public record SubmissionRequest(
        @Size(max = 3000, message = "Submission notes must not exceed 3000 characters")
        String submissionNotes,

        @Size(max = 500, message = "Attachment URL too long")
        String attachmentUrl
) {}
