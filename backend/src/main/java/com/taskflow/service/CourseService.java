package com.taskflow.service;

import com.taskflow.dto.CourseRequest;
import com.taskflow.dto.CourseResponse;
import com.taskflow.dto.EnrollmentRequest;
import com.taskflow.dto.StudentProgressResponse;
import com.taskflow.entity.*;
import com.taskflow.repository.*;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public CourseService(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            AssignmentRepository assignmentRepository,
            AssignmentSubmissionRepository submissionRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public CourseResponse createCourse(CourseRequest request) {
        User user = getCurrentUser();

        if (user.getRole() != Role.TEACHER && user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only teachers and admins can create courses");
        }

        if (courseRepository.existsByCode(request.code().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Course code already exists: " + request.code());
        }

        User teacher = user;
        if (user.getRole() == Role.ADMIN && request.teacherId() != null) {
            teacher = userRepository.findById(request.teacherId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));
            if (teacher.getRole() != Role.TEACHER) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected user is not a teacher");
            }
        }

        Course course = new Course(
                request.name().trim(),
                request.code().trim().toUpperCase(),
                request.description() != null ? request.description().trim() : null,
                teacher
        );

        Course saved = courseRepository.save(course);
        return toResponse(saved, user);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getAllCourses(String query) {
        User user = getCurrentUser();

        if (user.getRole() == Role.ADMIN) {
            return courseRepository.searchCourses(query).stream()
                    .map(c -> toResponse(c, user))
                    .toList();
        } else if (user.getRole() == Role.TEACHER) {
            return courseRepository.findByTeacher_Id(user.getId()).stream()
                    .filter(c -> query == null || c.getName().toLowerCase().contains(query.toLowerCase()) || c.getCode().toLowerCase().contains(query.toLowerCase()))
                    .map(c -> toResponse(c, user))
                    .toList();
        } else {
            // Student: return all active courses with enrollment flag
            List<Course> courses = courseRepository.findByActiveTrue();
            if (query != null && !query.isBlank()) {
                String q = query.toLowerCase();
                courses = courses.stream()
                        .filter(c -> c.getName().toLowerCase().contains(q) || c.getCode().toLowerCase().contains(q))
                        .toList();
            }
            return courses.stream().map(c -> toResponse(c, user)).toList();
        }
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourseById(Long id) {
        User user = getCurrentUser();
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (user.getRole() == Role.TEACHER && !course.getTeacher().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to access this course");
        }

        return toResponse(course, user);
    }

    @Transactional
    public CourseResponse updateCourse(Long id, CourseRequest request) {
        User user = getCurrentUser();
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        // Only owner teacher or admin
        if (user.getRole() != Role.ADMIN && !course.getTeacher().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the course teacher or an admin can update this course");
        }

        String newCode = request.code().trim().toUpperCase();
        if (!course.getCode().equalsIgnoreCase(newCode)) {
            if (courseRepository.existsByCode(newCode)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Course code already exists: " + newCode);
            }
            course.setCode(newCode);
        }

        course.setName(request.name().trim());
        if (request.description() != null) {
            course.setDescription(request.description().trim());
        }

        if (user.getRole() == Role.ADMIN && request.teacherId() != null) {
            User teacher = userRepository.findById(request.teacherId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));
            course.setTeacher(teacher);
        }

        Course saved = courseRepository.save(course);
        return toResponse(saved, user);
    }

    @Transactional
    public void deleteCourse(Long id) {
        User user = getCurrentUser();
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (user.getRole() != Role.ADMIN && !course.getTeacher().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the course teacher or an admin can delete this course");
        }

        courseRepository.delete(course);
    }

    @Transactional
    public void enrollStudent(Long courseId, EnrollmentRequest request) {
        User user = getCurrentUser();
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        User studentToEnroll;

        if (user.getRole() == Role.STUDENT) {
            // Student self-enrollment
            studentToEnroll = user;
        } else if (user.getRole() == Role.TEACHER) {
            // Teacher enrolling a student into their own course
            if (!course.getTeacher().getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teachers can only enroll students in their own courses");
            }
            studentToEnroll = findStudentFromRequest(request);
        } else if (user.getRole() == Role.ADMIN) {
            // Admin enrolling student
            studentToEnroll = findStudentFromRequest(request);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized");
        }

        if (studentToEnroll.getRole() != Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only users with STUDENT role can be enrolled in classes");
        }

        if (enrollmentRepository.existsByCourse_IdAndStudent_Id(course.getId(), studentToEnroll.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Student is already enrolled in this course");
        }

        Enrollment enrollment = new Enrollment(course, studentToEnroll);
        enrollmentRepository.save(enrollment);

        // Send in-app notification to the student
        notificationService.createNotification(
                studentToEnroll,
                "Enrolled in " + course.getCode(),
                "You have been successfully enrolled in " + course.getName() + " (" + course.getCode() + ").",
                "ASSIGNMENT",
                "/courses/" + course.getId()
        );
    }

    @Transactional
    public void unenrollStudent(Long courseId, Long studentId) {
        User user = getCurrentUser();
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        Long targetStudentId = studentId;
        if (user.getRole() == Role.STUDENT) {
            targetStudentId = user.getId();
        } else if (user.getRole() == Role.TEACHER) {
            if (!course.getTeacher().getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teachers can only unenroll students from their own courses");
            }
        }

        if (targetStudentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student ID is required");
        }

        enrollmentRepository.deleteByCourse_IdAndStudent_Id(courseId, targetStudentId);
    }

    @Transactional(readOnly = true)
    public List<StudentProgressResponse> getEnrolledStudents(Long courseId) {
        User user = getCurrentUser();
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (user.getRole() == Role.TEACHER && !course.getTeacher().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to view student progress for this course");
        }

        List<Enrollment> enrollments = enrollmentRepository.findByCourse_Id(courseId);
        long totalAssignments = assignmentRepository.countByCourse_Id(courseId);

        return enrollments.stream().map(e -> {
            User student = e.getStudent();
            List<AssignmentSubmission> submissions = submissionRepository.findByAssignment_Course_Id(courseId)
                    .stream()
                    .filter(s -> s.getStudent().getId().equals(student.getId()))
                    .toList();

            long completed = submissions.stream().filter(s -> !"IN_PROGRESS".equalsIgnoreCase(s.getStatus())).count();
            OptionalDouble avg = submissions.stream()
                    .filter(s -> s.getGrade() != null)
                    .mapToDouble(AssignmentSubmission::getGrade)
                    .average();

            Double avgGrade = avg.isPresent() ? Math.round(avg.getAsDouble() * 10.0) / 10.0 : null;

            return new StudentProgressResponse(
                    student.getId(),
                    student.getName(),
                    student.getEmail(),
                    e.getEnrolledAt().toString(),
                    completed,
                    totalAssignments,
                    avgGrade
            );
        }).toList();
    }

    private User findStudentFromRequest(EnrollmentRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enrollment request data is missing");
        }
        if (request.studentId() != null) {
            return userRepository.findById(request.studentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found with ID: " + request.studentId()));
        }
        if (request.studentEmail() != null && !request.studentEmail().isBlank()) {
            return userRepository.findByEmail(request.studentEmail().trim())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found with email: " + request.studentEmail()));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student ID or email is required");
    }

    private CourseResponse toResponse(Course c, User currentUser) {
        long studentCount = enrollmentRepository.countByCourse_Id(c.getId());
        long assignmentCount = assignmentRepository.countByCourse_Id(c.getId());
        Boolean isEnrolled = null;
        if (currentUser.getRole() == Role.STUDENT) {
            isEnrolled = enrollmentRepository.existsByCourse_IdAndStudent_Id(c.getId(), currentUser.getId());
        }

        return new CourseResponse(
                c.getId(),
                c.getName(),
                c.getCode(),
                c.getDescription(),
                c.getTeacher().getId(),
                c.getTeacher().getName(),
                c.getTeacher().getEmail(),
                c.isActive(),
                studentCount,
                assignmentCount,
                c.getCreatedAt() != null ? c.getCreatedAt().toString() : "",
                isEnrolled
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
