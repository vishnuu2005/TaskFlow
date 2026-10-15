package com.taskflow.service;

import com.taskflow.dto.AssignmentRequest;
import com.taskflow.dto.AssignmentResponse;
import com.taskflow.dto.GradeRequest;
import com.taskflow.dto.SubmissionRequest;
import com.taskflow.dto.SubmissionResponse;
import com.taskflow.entity.*;
import com.taskflow.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private AssignmentSubmissionRepository submissionRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AssignmentService assignmentService;

    private User teacher;
    private User otherTeacher;
    private User student;
    private Course course;

    @BeforeEach
    void setUp() {
        teacher = new User();
        teacher.setId(10L);
        teacher.setName("Teacher Alan");
        teacher.setEmail("alan@example.com");
        teacher.setRole(Role.TEACHER);

        otherTeacher = new User();
        otherTeacher.setId(20L);
        otherTeacher.setName("Teacher Bob");
        otherTeacher.setEmail("bob@example.com");
        otherTeacher.setRole(Role.TEACHER);

        student = new User();
        student.setId(30L);
        student.setName("Student Ada");
        student.setEmail("ada@example.com");
        student.setRole(Role.STUDENT);

        course = new Course("Computer Systems", "CS200", "Desc", teacher);
        course.setId(100L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getEmail(), "credentials", List.of())
        );
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    @Test
    void teacherCanCreateAssignmentInTheirCourse() {
        authenticateAs(teacher);
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(inv -> {
            Assignment a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });
        when(enrollmentRepository.findByCourse_Id(100L)).thenReturn(List.of());

        AssignmentRequest req = new AssignmentRequest(
                "Assembly Lab", "Write MIPS code", LocalDate.now().plusDays(7), "HIGH", 100, null
        );

        AssignmentResponse resp = assignmentService.createAssignment(100L, req);

        assertNotNull(resp);
        assertEquals("Assembly Lab", resp.title());
        assertEquals(100, resp.points());
        verify(assignmentRepository).save(any(Assignment.class));
    }

    @Test
    void otherTeacherCannotCreateAssignmentInCourse() {
        authenticateAs(otherTeacher);
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));

        AssignmentRequest req = new AssignmentRequest(
                "Sneaky Assignment", "Instructions", LocalDate.now().plusDays(7), "LOW", 50, null
        );

        assertThrows(ResponseStatusException.class, () -> assignmentService.createAssignment(100L, req));
        verify(assignmentRepository, never()).save(any(Assignment.class));
    }

    @Test
    void enrolledStudentCanSubmitAssignment() {
        authenticateAs(student);
        Assignment assignment = new Assignment("Lab 1", "Desc", LocalDate.now().plusDays(3), "MEDIUM", 100, null, course);
        assignment.setId(5L);

        when(assignmentRepository.findById(5L)).thenReturn(Optional.of(assignment));
        when(enrollmentRepository.existsByCourse_IdAndStudent_Id(100L, 30L)).thenReturn(true);
        when(submissionRepository.findByAssignment_IdAndStudent_Id(5L, 30L)).thenReturn(Optional.empty());
        when(submissionRepository.save(any(AssignmentSubmission.class))).thenAnswer(inv -> {
            AssignmentSubmission s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });

        SubmissionRequest req = new SubmissionRequest("My solution notes", "https://github.com/my-solution");
        SubmissionResponse resp = assignmentService.submitAssignment(5L, req);

        assertNotNull(resp);
        assertEquals("SUBMITTED", resp.status());
        assertEquals("My solution notes", resp.submissionNotes());
        verify(notificationService).createNotification(eq(teacher), anyString(), anyString(), eq("ASSIGNMENT"), anyString());
    }

    @Test
    void notEnrolledStudentCannotSubmitAssignment() {
        authenticateAs(student);
        Assignment assignment = new Assignment("Lab 1", "Desc", LocalDate.now().plusDays(3), "MEDIUM", 100, null, course);
        assignment.setId(5L);

        when(assignmentRepository.findById(5L)).thenReturn(Optional.of(assignment));
        when(enrollmentRepository.existsByCourse_IdAndStudent_Id(100L, 30L)).thenReturn(false);

        SubmissionRequest req = new SubmissionRequest("Notes", null);
        assertThrows(ResponseStatusException.class, () -> assignmentService.submitAssignment(5L, req));
    }

    @Test
    void teacherCanGradeSubmission() {
        authenticateAs(teacher);
        Assignment assignment = new Assignment("Lab 1", "Desc", LocalDate.now().plusDays(3), "MEDIUM", 100, null, course);
        assignment.setId(5L);

        AssignmentSubmission sub = new AssignmentSubmission(assignment, student, "Student notes", null);
        sub.setId(50L);

        when(assignmentRepository.findById(5L)).thenReturn(Optional.of(assignment));
        when(submissionRepository.findById(50L)).thenReturn(Optional.of(sub));
        when(submissionRepository.save(any(AssignmentSubmission.class))).thenAnswer(inv -> inv.getArgument(0));

        GradeRequest gradeReq = new GradeRequest(92.5, "Great work!");
        SubmissionResponse resp = assignmentService.gradeSubmission(5L, 50L, gradeReq);

        assertNotNull(resp);
        assertEquals(92.5, resp.grade());
        assertEquals("Great work!", resp.feedback());
        assertEquals("GRADED", resp.status());
        verify(notificationService).createNotification(eq(student), anyString(), anyString(), eq("FEEDBACK"), anyString());
    }
}
