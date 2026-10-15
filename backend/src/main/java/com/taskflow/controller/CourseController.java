package com.taskflow.controller;

import com.taskflow.dto.CourseRequest;
import com.taskflow.dto.CourseResponse;
import com.taskflow.dto.EnrollmentRequest;
import com.taskflow.dto.StudentProgressResponse;
import com.taskflow.service.CourseService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ResponseEntity<List<CourseResponse>> getAllCourses(
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(courseService.getAllCourses(query));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getCourseById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CourseRequest request) {
        CourseResponse response = courseService.createCourse(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseResponse> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseRequest request) {
        return ResponseEntity.ok(courseService.updateCourse(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.ok(Map.of("message", "Course deleted successfully"));
    }

    @PostMapping("/{id}/enroll")
    public ResponseEntity<Map<String, String>> enrollStudent(
            @PathVariable Long id,
            @RequestBody(required = false) EnrollmentRequest request) {
        courseService.enrollStudent(id, request);
        return ResponseEntity.ok(Map.of("message", "Enrollment successful"));
    }

    @DeleteMapping("/{id}/enroll/{studentId}")
    public ResponseEntity<Map<String, String>> unenrollStudent(
            @PathVariable Long id,
            @PathVariable Long studentId) {
        courseService.unenrollStudent(id, studentId);
        return ResponseEntity.ok(Map.of("message", "Unenrollment successful"));
    }

    @GetMapping("/{id}/students")
    public ResponseEntity<List<StudentProgressResponse>> getEnrolledStudents(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getEnrolledStudents(id));
    }
}
