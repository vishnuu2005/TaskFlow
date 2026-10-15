package com.taskflow.service;

import com.taskflow.dto.CourseRequest;
import com.taskflow.dto.CourseResponse;
import com.taskflow.dto.EnrollmentRequest;
import com.taskflow.entity.Course;
import com.taskflow.entity.Enrollment;
import com.taskflow.entity.Role;
import com.taskflow.entity.User;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private AssignmentSubmissionRepository submissionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CourseService courseService;

    private User teacher;
    private User otherTeacher;
    private User student;

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
    void teacherCanCreateCourse() {
        authenticateAs(teacher);
        CourseRequest req = new CourseRequest("Intro to AI", "AI101", "AI Basics", null);

        when(courseRepository.existsByCode("AI101")).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> {
            Course c = inv.getArgument(0);
            c.setId(100L);
            return c;
        });

        CourseResponse resp = courseService.createCourse(req);

        assertNotNull(resp);
        assertEquals("AI101", resp.code());
        assertEquals("Intro to AI", resp.name());
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void studentCannotCreateCourse() {
        authenticateAs(student);
        CourseRequest req = new CourseRequest("Hacking 101", "HACK", "Desc", null);

        assertThrows(ResponseStatusException.class, () -> courseService.createCourse(req));
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void teacherCannotUpdateOtherTeachersCourse() {
        authenticateAs(teacher);
        Course otherCourse = new Course("Bob's Class", "BOB101", "Desc", otherTeacher);
        otherCourse.setId(50L);

        when(courseRepository.findById(50L)).thenReturn(Optional.of(otherCourse));

        CourseRequest req = new CourseRequest("Hijacked Class", "BOB101", "New Desc", null);

        assertThrows(ResponseStatusException.class, () -> courseService.updateCourse(50L, req));
    }

    @Test
    void teacherCanEnrollStudentInTheirCourse() {
        authenticateAs(teacher);
        Course myCourse = new Course("My Class", "CS101", "Desc", teacher);
        myCourse.setId(10L);

        when(courseRepository.findById(10L)).thenReturn(Optional.of(myCourse));
        when(userRepository.findById(30L)).thenReturn(Optional.of(student));
        when(enrollmentRepository.existsByCourse_IdAndStudent_Id(10L, 30L)).thenReturn(false);

        EnrollmentRequest req = new EnrollmentRequest(30L, null);
        assertDoesNotThrow(() -> courseService.enrollStudent(10L, req));

        verify(enrollmentRepository).save(any(Enrollment.class));
        verify(notificationService).createNotification(eq(student), anyString(), anyString(), eq("ASSIGNMENT"), anyString());
    }

    @Test
    void teacherCannotEnrollStudentInAnotherTeachersCourse() {
        authenticateAs(teacher);
        Course otherCourse = new Course("Bob's Class", "BOB101", "Desc", otherTeacher);
        otherCourse.setId(20L);

        when(courseRepository.findById(20L)).thenReturn(Optional.of(otherCourse));

        EnrollmentRequest req = new EnrollmentRequest(30L, null);
        assertThrows(ResponseStatusException.class, () -> courseService.enrollStudent(20L, req));
    }
}
