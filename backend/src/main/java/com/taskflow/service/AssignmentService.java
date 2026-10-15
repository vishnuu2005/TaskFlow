package com.taskflow.service;

import com.taskflow.dto.AssignmentRequest;
import com.taskflow.dto.AssignmentResponse;
import com.taskflow.dto.GradeRequest;
import com.taskflow.dto.SubmissionRequest;
import com.taskflow.dto.SubmissionResponse;
import com.taskflow.entity.*;
import com.taskflow.repository.*;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            AssignmentSubmissionRepository submissionRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public AssignmentResponse createAssignment(Long courseId, AssignmentRequest request) {
        User user = getCurrentUser();
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (user.getRole() != Role.ADMIN && !course.getTeacher().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the course teacher or an admin can create assignments");
        }

        Assignment assignment = new Assignment(
                request.title().trim(),
                request.instructions() != null ? request.instructions().trim() : null,
                request.dueDate(),
                request.priority() != null ? request.priority() : "MEDIUM",
                request.points() != null ? request.points() : 100,
                request.attachmentUrl(),
                course
        );

        Assignment saved = assignmentRepository.save(assignment);

        // Notify enrolled students
        List<Enrollment> enrollments = enrollmentRepository.findByCourse_Id(courseId);
        List<User> students = enrollments.stream().map(Enrollment::getStudent).toList();
        String dueStr = saved.getDueDate() != null ? " (Due: " + saved.getDueDate() + ")" : "";
        notificationService.notifyUsers(
                students,
                "New Assignment: " + saved.getTitle(),
                "A new assignment has been posted in " + course.getName() + dueStr,
                "ASSIGNMENT",
                "/assignments/" + saved.getId()
        );

        return toResponse(saved, user);
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getCourseAssignments(Long courseId) {
        User user = getCurrentUser();
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (user.getRole() == Role.STUDENT) {
            boolean enrolled = enrollmentRepository.existsByCourse_IdAndStudent_Id(courseId, user.getId());
            if (!enrolled) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must be enrolled in this course to view its assignments");
            }
        } else if (user.getRole() == Role.TEACHER) {
            if (!course.getTeacher().getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to view assignments for this course");
            }
        }

        return assignmentRepository.findByCourse_IdOrderByDueDateAsc(courseId).stream()
                .map(a -> toResponse(a, user))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAllMyAssignments() {
        User user = getCurrentUser();

        if (user.getRole() == Role.ADMIN) {
            return assignmentRepository.findAll().stream()
                    .map(a -> toResponse(a, user))
                    .toList();
        } else if (user.getRole() == Role.TEACHER) {
            return assignmentRepository.findByCourse_Teacher_IdOrderByDueDateAsc(user.getId()).stream()
                    .map(a -> toResponse(a, user))
                    .toList();
        } else {
            // Student: assignments for enrolled courses
            List<Long> enrolledCourseIds = enrollmentRepository.findByStudent_Id(user.getId())
                    .stream()
                    .map(e -> e.getCourse().getId())
                    .toList();

            if (enrolledCourseIds.isEmpty()) {
                return List.of();
            }

            return assignmentRepository.findByCourse_IdInOrderByDueDateAsc(enrolledCourseIds).stream()
                    .map(a -> toResponse(a, user))
                    .toList();
        }
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getAssignmentById(Long id) {
        User user = getCurrentUser();
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        if (user.getRole() == Role.STUDENT) {
            boolean enrolled = enrollmentRepository.existsByCourse_IdAndStudent_Id(assignment.getCourse().getId(), user.getId());
            if (!enrolled) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must be enrolled in this course to view this assignment");
            }
        } else if (user.getRole() == Role.TEACHER) {
            if (!assignment.getCourse().getTeacher().getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to view this assignment");
            }
        }

        return toResponse(assignment, user);
    }

    @Transactional
    public AssignmentResponse updateAssignment(Long id, AssignmentRequest request) {
        User user = getCurrentUser();
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        if (user.getRole() != Role.ADMIN && !assignment.getCourse().getTeacher().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the course teacher or an admin can update this assignment");
        }

        assignment.setTitle(request.title().trim());
        if (request.instructions() != null) {
            assignment.setInstructions(request.instructions().trim());
        }
        assignment.setDueDate(request.dueDate());
        if (request.priority() != null) {
            assignment.setPriority(request.priority());
        }
        if (request.points() != null) {
            assignment.setPoints(request.points());
        }
        if (request.attachmentUrl() != null) {
            assignment.setAttachmentUrl(request.attachmentUrl());
        }

        Assignment updated = assignmentRepository.save(assignment);
        return toResponse(updated, user);
    }

    @Transactional
    public void deleteAssignment(Long id) {
        User user = getCurrentUser();
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        if (user.getRole() != Role.ADMIN && !assignment.getCourse().getTeacher().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the course teacher or an admin can delete this assignment");
        }

        assignmentRepository.delete(assignment);
    }

    @Transactional
    public SubmissionResponse submitAssignment(Long id, SubmissionRequest request) {
        User user = getCurrentUser();
        if (user.getRole() != Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only students can submit assignments");
        }

        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        boolean enrolled = enrollmentRepository.existsByCourse_IdAndStudent_Id(assignment.getCourse().getId(), user.getId());
        if (!enrolled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not enrolled in the course for this assignment");
        }

        AssignmentSubmission submission = submissionRepository.findByAssignment_IdAndStudent_Id(id, user.getId())
                .orElse(new AssignmentSubmission());

        submission.setAssignment(assignment);
        submission.setStudent(user);
        submission.setSubmissionNotes(request.submissionNotes());
        submission.setAttachmentUrl(request.attachmentUrl());
        submission.setStatus("SUBMITTED");
        submission.setSubmittedAt(LocalDateTime.now());

        AssignmentSubmission saved = submissionRepository.save(submission);

        // Notify teacher
        notificationService.createNotification(
                assignment.getCourse().getTeacher(),
                "New Submission: " + assignment.getTitle(),
                user.getName() + " submitted work for " + assignment.getTitle(),
                "ASSIGNMENT",
                "/assignments/" + assignment.getId() + "/submissions"
        );

        return toSubmissionResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getAssignmentSubmissions(Long assignmentId) {
        User user = getCurrentUser();
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        if (user.getRole() != Role.ADMIN && !assignment.getCourse().getTeacher().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the course teacher or an admin can view student submissions");
        }

        return submissionRepository.findByAssignment_Id(assignmentId).stream()
                .map(this::toSubmissionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubmissionResponse getMySubmission(Long assignmentId) {
        User user = getCurrentUser();
        return submissionRepository.findByAssignment_IdAndStudent_Id(assignmentId, user.getId())
                .map(this::toSubmissionResponse)
                .orElse(null);
    }

    @Transactional
    public SubmissionResponse gradeSubmission(Long assignmentId, Long submissionId, GradeRequest request) {
        User user = getCurrentUser();
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        if (user.getRole() != Role.ADMIN && !assignment.getCourse().getTeacher().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the course teacher or an admin can grade submissions");
        }

        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));

        if (!submission.getAssignment().getId().equals(assignmentId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Submission does not match assignment");
        }

        submission.setGrade(request.grade());
        submission.setFeedback(request.feedback() != null ? request.feedback().trim() : null);
        submission.setFeedbackAt(LocalDateTime.now());
        submission.setStatus("GRADED");

        AssignmentSubmission saved = submissionRepository.save(submission);

        // Notify student of grade and feedback
        notificationService.createNotification(
                submission.getStudent(),
                "Grade & Feedback: " + assignment.getTitle(),
                "Your submission was graded: " + request.grade() + "/" + assignment.getPoints() +
                        (request.feedback() != null ? " - Feedback: " + request.feedback() : ""),
                "FEEDBACK",
                "/assignments/" + assignment.getId()
        );

        return toSubmissionResponse(saved);
    }

    private AssignmentResponse toResponse(Assignment a, User user) {
        String submissionStatus = "NOT_SUBMITTED";
        Double myGrade = null;
        String myFeedback = null;
        String mySubmissionNotes = null;
        String submittedAt = null;

        if (user.getRole() == Role.STUDENT) {
            Optional<AssignmentSubmission> subOpt = submissionRepository.findByAssignment_IdAndStudent_Id(a.getId(), user.getId());
            if (subOpt.isPresent()) {
                AssignmentSubmission s = subOpt.get();
                submissionStatus = s.getStatus();
                myGrade = s.getGrade();
                myFeedback = s.getFeedback();
                mySubmissionNotes = s.getSubmissionNotes();
                submittedAt = s.getSubmittedAt() != null ? s.getSubmittedAt().toString() : null;
            }
        }

        Long totalSubmissions = null;
        Long gradedSubmissions = null;
        if (user.getRole() == Role.TEACHER || user.getRole() == Role.ADMIN) {
            totalSubmissions = submissionRepository.countByAssignment_Id(a.getId());
            gradedSubmissions = (long) submissionRepository.findByAssignment_Id(a.getId()).stream()
                    .filter(s -> "GRADED".equalsIgnoreCase(s.getStatus()))
                    .count();
        }

        return new AssignmentResponse(
                a.getId(),
                a.getTitle(),
                a.getInstructions(),
                a.getDueDate(),
                a.getPriority(),
                a.getPoints(),
                a.getAttachmentUrl(),
                a.getCourse().getId(),
                a.getCourse().getName(),
                a.getCourse().getCode(),
                a.getCourse().getTeacher().getName(),
                submissionStatus,
                myGrade,
                myFeedback,
                mySubmissionNotes,
                submittedAt,
                totalSubmissions,
                gradedSubmissions
        );
    }

    private SubmissionResponse toSubmissionResponse(AssignmentSubmission s) {
        return new SubmissionResponse(
                s.getId(),
                s.getAssignment().getId(),
                s.getAssignment().getTitle(),
                s.getAssignment().getPoints(),
                s.getStudent().getId(),
                s.getStudent().getName(),
                s.getStudent().getEmail(),
                s.getSubmissionNotes(),
                s.getAttachmentUrl(),
                s.getStatus(),
                s.getSubmittedAt() != null ? s.getSubmittedAt().toString() : "",
                s.getGrade(),
                s.getFeedback(),
                s.getFeedbackAt() != null ? s.getFeedbackAt().toString() : null
        );
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
