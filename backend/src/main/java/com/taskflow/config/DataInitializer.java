package com.taskflow.config;

import com.taskflow.entity.*;
import com.taskflow.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final AnnouncementRepository announcementRepository;
    private final NotificationRepository notificationRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    public DataInitializer(
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            AssignmentRepository assignmentRepository,
            AssignmentSubmissionRepository submissionRepository,
            AnnouncementRepository announcementRepository,
            NotificationRepository notificationRepository,
            TaskRepository taskRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.announcementRepository = announcementRepository;
        this.notificationRepository = notificationRepository;
        this.taskRepository = taskRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Safe check: Ensure at least one ADMIN exists
        User admin = userRepository.findByEmail("admin@taskflow.com").orElse(null);
        if (admin == null && !userRepository.existsByRole(Role.ADMIN)) {
            admin = new User();
            admin.setName("System Administrator");
            admin.setEmail("admin@taskflow.com");
            admin.setPassword(passwordEncoder.encode("AdminPassword123!"));
            admin.setRole(Role.ADMIN);
            admin.setActive(true);
            admin = userRepository.save(admin);
            log.info("Initialized default administrator: admin@taskflow.com");
        }

        // Initialize default categories if none exist
        if (categoryRepository.count() == 0) {
            categoryRepository.save(new Category("Homework", "Daily class homework and reading", "#8b5cf6"));
            categoryRepository.save(new Category("Project", "Term projects and lab coursework", "#3b82f6"));
            categoryRepository.save(new Category("Exam", "Midterms, finals, and quizzes", "#ef4444"));
            categoryRepository.save(new Category("Personal", "Personal errands and study goals", "#10b981"));
            categoryRepository.save(new Category("Research", "Literature review and research papers", "#f59e0b"));
            log.info("Initialized standard categories");
        }

        // In dev or test profiles, seed demo teacher, student, course, and assignments
        if (activeProfile != null && (activeProfile.contains("dev") || activeProfile.contains("test"))) {
            User teacher = userRepository.findByEmail("teacher@taskflow.com").orElse(null);
            if (teacher == null) {
                teacher = new User();
                teacher.setName("Prof. Alan Turing");
                teacher.setEmail("teacher@taskflow.com");
                teacher.setPassword(passwordEncoder.encode("TeacherPassword123!"));
                teacher.setRole(Role.TEACHER);
                teacher.setActive(true);
                teacher = userRepository.save(teacher);
                log.info("Initialized demo teacher: teacher@taskflow.com");
            }

            User student = userRepository.findByEmail("student@taskflow.com").orElse(null);
            if (student == null) {
                student = new User();
                student.setName("Ada Lovelace");
                student.setEmail("student@taskflow.com");
                student.setPassword(passwordEncoder.encode("StudentPassword123!"));
                student.setRole(Role.STUDENT);
                student.setActive(true);
                student = userRepository.save(student);
                log.info("Initialized demo student: student@taskflow.com");
            }

            // Seed demo courses
            Course cs101 = courseRepository.findByCode("CS101").orElse(null);
            if (cs101 == null) {
                cs101 = new Course(
                        "Introduction to Computer Science",
                        "CS101",
                        "Fundamental principles of computation, algorithms, data structures, and software engineering.",
                        teacher
                );
                cs101 = courseRepository.save(cs101);
                log.info("Initialized course CS101");
            }

            Course web201 = courseRepository.findByCode("WEB201").orElse(null);
            if (web201 == null) {
                web201 = new Course(
                        "Full-Stack Web Development",
                        "WEB201",
                        "Modern web applications with Spring Boot backend, REST APIs, and responsive glassmorphism UI.",
                        teacher
                );
                web201 = courseRepository.save(web201);
                log.info("Initialized course WEB201");
            }

            // Enroll student
            if (!enrollmentRepository.existsByCourse_IdAndStudent_Id(cs101.getId(), student.getId())) {
                enrollmentRepository.save(new Enrollment(cs101, student));
                log.info("Enrolled student into CS101");
            }
            if (!enrollmentRepository.existsByCourse_IdAndStudent_Id(web201.getId(), student.getId())) {
                enrollmentRepository.save(new Enrollment(web201, student));
                log.info("Enrolled student into WEB201");
            }

            // Seed assignments
            if (assignmentRepository.countByCourse_Id(cs101.getId()) == 0) {
                Assignment a1 = new Assignment(
                        "Data Structures & Big-O Notation",
                        "Analyze the time and space complexity of sorting algorithms and implement a red-black tree in Java.",
                        LocalDate.now().plusDays(5),
                        "HIGH",
                        100,
                        "https://example.com/cs101-trees.pdf",
                        cs101
                );
                a1 = assignmentRepository.save(a1);

                Assignment a2 = new Assignment(
                        "Algorithmic Problem Solving",
                        "Solve five dynamic programming challenges on recursion, memoization, and tabulations.",
                        LocalDate.now().plusDays(1),
                        "MEDIUM",
                        50,
                        null,
                        cs101
                );
                assignmentRepository.save(a2);

                // Sample submission with feedback
                AssignmentSubmission sub1 = new AssignmentSubmission(
                        a1,
                        student,
                        "Implemented Red-Black Tree with rotation tests in JUnit 5. All 25 test cases passing with O(log n) search.",
                        "https://github.com/example/cs101-rbt"
                );
                sub1.setStatus("GRADED");
                sub1.setGrade(95.0);
                sub1.setFeedback("Superb implementation! Your explanation of edge-case balancing rotations was clear and precise.");
                sub1.setFeedbackAt(LocalDateTime.now().minusDays(1));
                submissionRepository.save(sub1);
                log.info("Seeded CS101 assignments and submission");
            }

            if (assignmentRepository.countByCourse_Id(web201.getId()) == 0) {
                Assignment w1 = new Assignment(
                        "Build a Secure Glassmorphism Dashboard",
                        "Design and implement a responsive role-based learning management system with dark glass aesthetic and JWT authentication.",
                        LocalDate.now().plusDays(8),
                        "HIGH",
                        100,
                        null,
                        web201
                );
                assignmentRepository.save(w1);
                log.info("Seeded WEB201 assignments");
            }

            // Seed announcements
            if (announcementRepository.count() == 0) {
                if (admin != null) {
                    announcementRepository.save(new Announcement(
                            "Welcome to TaskFlow LMS Platform",
                            "TaskFlow now supports comprehensive role-based access for Admins, Teachers, and Students. Check your calendar for upcoming deadlines!",
                            null,
                            admin,
                            "HIGH"
                    ));
                }
                announcementRepository.save(new Announcement(
                        "Midterm Review Session",
                        "Prof. Turing will host an interactive midterm exam review this Thursday at 4 PM in Auditorium B.",
                        cs101,
                        teacher,
                        "NORMAL"
                ));
                log.info("Seeded demo announcements");
            }

            // Seed sample personal tasks for student
            if (taskRepository.countByUser_Id(student.getId()) == 0) {
                Task t1 = new Task();
                t1.setTitle("Review Discrete Mathematics Chapter 4");
                t1.setDescription("Read proof techniques and complete practice exercises 4.1 through 4.8.");
                t1.setPriority("HIGH");
                t1.setCategory("Homework");
                t1.setDueDate(LocalDate.now().plusDays(2));
                t1.setUser(student);
                t1.addSubTask(new SubTask("Read section 4.1 Direct Proofs", true, t1));
                t1.addSubTask(new SubTask("Read section 4.2 Induction", false, t1));
                t1.addSubTask(new SubTask("Solve practice exercises", false, t1));
                taskRepository.save(t1);

                Task t2 = new Task();
                t2.setTitle("Set up development environment for Spring Boot");
                t2.setDescription("Install JDK 21, configure Maven wrapper, and verify H2 console.");
                t2.setPriority("MEDIUM");
                t2.setCategory("Personal");
                t2.setDueDate(LocalDate.now().minusDays(1));
                t2.setCompleted(true);
                t2.setUser(student);
                taskRepository.save(t2);

                log.info("Seeded sample student tasks with subtasks");
            }

            // Seed initial notification
            if (notificationRepository.countByUser_IdAndIsReadFalse(student.getId()) == 0) {
                notificationRepository.save(new Notification(
                        student,
                        "Welcome to TaskFlow!",
                        "You are enrolled in Introduction to Computer Science (CS101) and Web Development (WEB201).",
                        "SYSTEM",
                        "/courses"
                ));
            }
        }
    }
}
