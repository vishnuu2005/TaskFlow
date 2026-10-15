package com.taskflow.service;

import com.taskflow.dto.AnnouncementResponse;
import com.taskflow.dto.AssignmentResponse;
import com.taskflow.dto.SubmissionResponse;
import com.taskflow.dto.UserSummaryResponse;
import com.taskflow.entity.*;
import com.taskflow.repository.*;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final TaskRepository taskRepository;
    private final AnnouncementRepository announcementRepository;

    public DashboardService(
            UserRepository userRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            AssignmentRepository assignmentRepository,
            AssignmentSubmissionRepository submissionRepository,
            TaskRepository taskRepository,
            AnnouncementRepository announcementRepository) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.taskRepository = taskRepository;
        this.announcementRepository = announcementRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminDashboard() {
        User user = getCurrentUser();
        if (user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }

        long totalUsers = userRepository.count();
        long adminCount = userRepository.countByRole(Role.ADMIN);
        long teacherCount = userRepository.countByRole(Role.TEACHER);
        long studentCount = userRepository.countByRole(Role.STUDENT);
        long activeCount = userRepository.countByActiveTrue();
        long inactiveCount = userRepository.countByActiveFalse();

        long totalCourses = courseRepository.count();
        long totalAssignments = assignmentRepository.count();
        long totalSubmissions = submissionRepository.count();
        long totalPersonalTasks = taskRepository.count();

        List<AssignmentSubmission> allSubs = submissionRepository.findAll();
        long gradedSubs = allSubs.stream().filter(s -> "GRADED".equalsIgnoreCase(s.getStatus())).count();
        long completionRate = totalSubmissions > 0 ? (gradedSubs * 100 / totalSubmissions) : 0;

        List<UserSummaryResponse> recentUsers = userRepository.findAll().stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .limit(5)
                .map(UserSummaryResponse::fromEntity)
                .toList();

        List<Map<String, Object>> recentAnnouncements = announcementRepository.findAllOrdered().stream()
                .limit(5)
                .map(a -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", a.getId());
                    m.put("title", a.getTitle());
                    m.put("authorName", a.getAuthor().getName());
                    m.put("courseName", a.getCourse() != null ? a.getCourse().getName() : "Platform");
                    m.put("createdAt", a.getCreatedAt().toString());
                    return m;
                })
                .toList();

        Map<String, Object> res = new HashMap<>();
        res.put("totalUsers", totalUsers);
        res.put("adminCount", adminCount);
        res.put("teacherCount", teacherCount);
        res.put("studentCount", studentCount);
        res.put("activeCount", activeCount);
        res.put("inactiveCount", inactiveCount);
        res.put("totalCourses", totalCourses);
        res.put("totalAssignments", totalAssignments);
        res.put("totalSubmissions", totalSubmissions);
        res.put("gradedSubmissions", gradedSubs);
        res.put("completionRate", completionRate);
        res.put("totalPersonalTasks", totalPersonalTasks);
        res.put("recentUsers", recentUsers);
        res.put("recentAnnouncements", recentAnnouncements);

        return res;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTeacherDashboard() {
        User user = getCurrentUser();
        if (user.getRole() != Role.TEACHER && user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teacher access required");
        }

        Long teacherId = user.getId();
        long totalClasses = courseRepository.countByTeacher_Id(teacherId);
        long totalStudents = enrollmentRepository.countByCourse_Teacher_Id(teacherId);
        long totalAssignments = assignmentRepository.countByCourse_Teacher_Id(teacherId);
        long totalSubmissions = submissionRepository.countByAssignment_Course_Teacher_Id(teacherId);
        long pendingSubmissions = submissionRepository.countByAssignment_Course_Teacher_IdAndStatus(teacherId, "SUBMITTED");
        long gradedSubmissions = submissionRepository.countByAssignment_Course_Teacher_IdAndStatus(teacherId, "GRADED");

        long completionRate = totalSubmissions > 0 ? (gradedSubmissions * 100 / totalSubmissions) : 0;

        LocalDate today = LocalDate.now();
        List<Map<String, Object>> upcomingAssignments = assignmentRepository
                .findUpcomingForTeacher(teacherId, today, today.plusDays(14))
                .stream()
                .map(a -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", a.getId());
                    m.put("title", a.getTitle());
                    m.put("courseName", a.getCourse().getName());
                    m.put("courseCode", a.getCourse().getCode());
                    m.put("dueDate", a.getDueDate() != null ? a.getDueDate().toString() : null);
                    m.put("priority", a.getPriority());
                    return m;
                })
                .toList();

        List<SubmissionResponse> pendingList = submissionRepository.findPendingSubmissionsForTeacher(teacherId).stream()
                .limit(6)
                .map(s -> new SubmissionResponse(
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
                        s.getSubmittedAt().toString(),
                        s.getGrade(),
                        s.getFeedback(),
                        s.getFeedbackAt() != null ? s.getFeedbackAt().toString() : null
                ))
                .toList();

        List<Course> courses = courseRepository.findByTeacher_Id(teacherId);
        List<Map<String, Object>> courseSummaries = courses.stream().map(c -> {
            Map<String, Object> cm = new HashMap<>();
            cm.put("id", c.getId());
            cm.put("name", c.getName());
            cm.put("code", c.getCode());
            cm.put("studentCount", enrollmentRepository.countByCourse_Id(c.getId()));
            cm.put("assignmentCount", assignmentRepository.countByCourse_Id(c.getId()));
            return cm;
        }).toList();

        Map<String, Object> res = new HashMap<>();
        res.put("teacherName", user.getName());
        res.put("totalClasses", totalClasses);
        res.put("totalStudents", totalStudents);
        res.put("totalAssignments", totalAssignments);
        res.put("totalSubmissions", totalSubmissions);
        res.put("pendingSubmissions", pendingSubmissions);
        res.put("gradedSubmissions", gradedSubmissions);
        res.put("completionRate", completionRate);
        res.put("upcomingAssignments", upcomingAssignments);
        res.put("pendingList", pendingList);
        res.put("courses", courseSummaries);

        return res;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStudentDashboard() {
        User user = getCurrentUser();
        Long studentId = user.getId();
        LocalDate today = LocalDate.now();

        // 1. Personal task stats
        long totalTasks = taskRepository.countByUser_Id(studentId);
        long completedTasks = taskRepository.countByUser_IdAndCompletedTrue(studentId);
        long pendingTasks = totalTasks - completedTasks;
        long overdueTasks = taskRepository.countByUser_IdAndCompletedFalseAndDueDateBefore(studentId, today);
        long taskCompletionRate = totalTasks > 0 ? (completedTasks * 100 / totalTasks) : 0;

        // 2. Class & assignments stats
        List<Enrollment> enrollments = enrollmentRepository.findByStudent_Id(studentId);
        long enrolledClassesCount = enrollments.size();
        List<Long> enrolledCourseIds = enrollments.stream().map(e -> e.getCourse().getId()).toList();

        List<Assignment> classAssignments = enrolledCourseIds.isEmpty()
                ? List.of()
                : assignmentRepository.findByCourse_IdIn(enrolledCourseIds);
        long totalAssignments = classAssignments.size();

        List<AssignmentSubmission> mySubmissions = submissionRepository.findByStudent_Id(studentId);
        long submittedCount = mySubmissions.stream().filter(s -> !"IN_PROGRESS".equalsIgnoreCase(s.getStatus())).count();
        long gradedCount = mySubmissions.stream().filter(s -> "GRADED".equalsIgnoreCase(s.getStatus())).count();
        long pendingAssignments = Math.max(0, totalAssignments - submittedCount);

        OptionalDouble avgGrade = mySubmissions.stream()
                .filter(s -> s.getGrade() != null)
                .mapToDouble(AssignmentSubmission::getGrade)
                .average();
        Double averageGrade = avgGrade.isPresent() ? Math.round(avgGrade.getAsDouble() * 10.0) / 10.0 : null;

        // 3. Upcoming due items (Personal tasks + assignments due in next 7 days)
        List<Map<String, Object>> upcomingItems = new ArrayList<>();
        List<Task> upcomingTasks = taskRepository.findByUser_IdAndDueDateBetween(studentId, today, today.plusDays(7));
        for (Task t : upcomingTasks) {
            if (!t.isCompleted()) {
                Map<String, Object> item = new HashMap<>();
                item.put("type", "TASK");
                item.put("id", t.getId());
                item.put("title", t.getTitle());
                item.put("dueDate", t.getDueDate().toString());
                item.put("priority", t.getPriority());
                item.put("category", t.getCategory());
                upcomingItems.add(item);
            }
        }

        if (!enrolledCourseIds.isEmpty()) {
            List<Assignment> upcomingAss = assignmentRepository.findUpcomingForCourses(enrolledCourseIds, today, today.plusDays(7));
            Set<Long> submittedAssIds = new HashSet<>();
            for (AssignmentSubmission s : mySubmissions) {
                submittedAssIds.add(s.getAssignment().getId());
            }

            for (Assignment a : upcomingAss) {
                if (!submittedAssIds.contains(a.getId())) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("type", "ASSIGNMENT");
                    item.put("id", a.getId());
                    item.put("title", a.getTitle());
                    item.put("courseName", a.getCourse().getName());
                    item.put("courseCode", a.getCourse().getCode());
                    item.put("dueDate", a.getDueDate() != null ? a.getDueDate().toString() : null);
                    item.put("priority", a.getPriority());
                    item.put("points", a.getPoints());
                    upcomingItems.add(item);
                }
            }
        }

        // 4. Recent feedback
        List<Map<String, Object>> recentFeedback = mySubmissions.stream()
                .filter(s -> s.getFeedback() != null && !s.getFeedback().isBlank())
                .sorted((a, b) -> {
                    if (a.getFeedbackAt() == null || b.getFeedbackAt() == null) return 0;
                    return b.getFeedbackAt().compareTo(a.getFeedbackAt());
                })
                .limit(5)
                .map(s -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("assignmentTitle", s.getAssignment().getTitle());
                    m.put("courseName", s.getAssignment().getCourse().getName());
                    m.put("grade", s.getGrade());
                    m.put("points", s.getAssignment().getPoints());
                    m.put("feedback", s.getFeedback());
                    m.put("feedbackAt", s.getFeedbackAt() != null ? s.getFeedbackAt().toString() : null);
                    return m;
                })
                .toList();

        Map<String, Object> res = new HashMap<>();
        res.put("studentName", user.getName());
        res.put("totalTasks", totalTasks);
        res.put("completedTasks", completedTasks);
        res.put("pendingTasks", pendingTasks);
        res.put("overdueTasks", overdueTasks);
        res.put("taskCompletionRate", taskCompletionRate);
        res.put("enrolledClassesCount", enrolledClassesCount);
        res.put("totalAssignments", totalAssignments);
        res.put("submittedAssignments", submittedCount);
        res.put("gradedAssignments", gradedCount);
        res.put("pendingAssignments", pendingAssignments);
        res.put("averageGrade", averageGrade);
        res.put("upcomingItems", upcomingItems);
        res.put("recentFeedback", recentFeedback);

        return res;
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
