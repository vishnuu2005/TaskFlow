package com.taskflow.controller;

import com.taskflow.dto.AssignmentRequest;
import com.taskflow.dto.AssignmentResponse;
import com.taskflow.dto.GradeRequest;
import com.taskflow.dto.SubmissionRequest;
import com.taskflow.dto.SubmissionResponse;
import com.taskflow.service.AssignmentService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping("/api/assignments")
    public ResponseEntity<List<AssignmentResponse>> getAllMyAssignments() {
        return ResponseEntity.ok(assignmentService.getAllMyAssignments());
    }

    @GetMapping("/api/courses/{courseId}/assignments")
    public ResponseEntity<List<AssignmentResponse>> getCourseAssignments(@PathVariable Long courseId) {
        return ResponseEntity.ok(assignmentService.getCourseAssignments(courseId));
    }

    @PostMapping("/api/courses/{courseId}/assignments")
    public ResponseEntity<AssignmentResponse> createAssignment(
            @PathVariable Long courseId,
            @Valid @RequestBody AssignmentRequest request) {
        AssignmentResponse response = assignmentService.createAssignment(courseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/assignments/{id}")
    public ResponseEntity<AssignmentResponse> getAssignmentById(@PathVariable Long id) {
        return ResponseEntity.ok(assignmentService.getAssignmentById(id));
    }

    @PutMapping("/api/assignments/{id}")
    public ResponseEntity<AssignmentResponse> updateAssignment(
            @PathVariable Long id,
            @Valid @RequestBody AssignmentRequest request) {
        return ResponseEntity.ok(assignmentService.updateAssignment(id, request));
    }

    @DeleteMapping("/api/assignments/{id}")
    public ResponseEntity<Map<String, String>> deleteAssignment(@PathVariable Long id) {
        assignmentService.deleteAssignment(id);
        return ResponseEntity.ok(Map.of("message", "Assignment deleted successfully"));
    }

    @PostMapping("/api/assignments/{id}/submit")
    public ResponseEntity<SubmissionResponse> submitAssignment(
            @PathVariable Long id,
            @Valid @RequestBody SubmissionRequest request) {
        return ResponseEntity.ok(assignmentService.submitAssignment(id, request));
    }

    @GetMapping("/api/assignments/{id}/my-submission")
    public ResponseEntity<SubmissionResponse> getMySubmission(@PathVariable Long id) {
        SubmissionResponse response = assignmentService.getMySubmission(id);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/assignments/{id}/submissions")
    public ResponseEntity<List<SubmissionResponse>> getAssignmentSubmissions(@PathVariable Long id) {
        return ResponseEntity.ok(assignmentService.getAssignmentSubmissions(id));
    }

    @PatchMapping("/api/assignments/{id}/submissions/{submissionId}/grade")
    public ResponseEntity<SubmissionResponse> gradeSubmission(
            @PathVariable Long id,
            @PathVariable Long submissionId,
            @Valid @RequestBody GradeRequest request) {
        return ResponseEntity.ok(assignmentService.gradeSubmission(id, submissionId, request));
    }
}
