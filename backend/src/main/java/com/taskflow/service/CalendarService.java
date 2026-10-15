package com.taskflow.service;

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
public class CalendarService {

    private final TaskRepository taskRepository;
    private final AssignmentRepository assignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    public CalendarService(
            TaskRepository taskRepository,
            AssignmentRepository assignmentRepository,
            EnrollmentRepository enrollmentRepository,
            AssignmentSubmissionRepository submissionRepository,
            UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.assignmentRepository = assignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCalendarEvents(String startStr, String endStr) {
        User user = getCurrentUser();
        LocalDate start = (startStr != null && !startStr.isBlank()) ? LocalDate.parse(startStr) : LocalDate.now().minusMonths(1);
        LocalDate end = (endStr != null && !endStr.isBlank()) ? LocalDate.parse(endStr) : LocalDate.now().plusMonths(2);

        List<Map<String, Object>> events = new ArrayList<>();

        // 1. Personal tasks with due dates
        List<Task> tasks = taskRepository.findByUser_IdAndDueDateBetween(user.getId(), start, end);
        for (Task t : tasks) {
            Map<String, Object> ev = new HashMap<>();
            ev.put("id", "task-" + t.getId());
            ev.put("rawId", t.getId());
            ev.put("title", t.getTitle());
            ev.put("type", "TASK");
            ev.put("date", t.getDueDate().toString());
            ev.put("priority", t.getPriority());
            ev.put("completed", t.isCompleted());
            ev.put("category", t.getCategory());
            events.add(ev);
        }

        // 2. Class assignments based on role
        if (user.getRole() == Role.STUDENT) {
            List<Long> courseIds = enrollmentRepository.findByStudent_Id(user.getId()).stream()
                    .map(e -> e.getCourse().getId())
                    .toList();

            if (!courseIds.isEmpty()) {
                List<Assignment> assignments = assignmentRepository.findUpcomingForCourses(courseIds, start, end);
                for (Assignment a : assignments) {
                    boolean submitted = submissionRepository.existsByAssignment_IdAndStudent_Id(a.getId(), user.getId());
                    Map<String, Object> ev = new HashMap<>();
                    ev.put("id", "assignment-" + a.getId());
                    ev.put("rawId", a.getId());
                    ev.put("title", a.getTitle());
                    ev.put("type", "ASSIGNMENT");
                    ev.put("date", a.getDueDate() != null ? a.getDueDate().toString() : null);
                    ev.put("priority", a.getPriority());
                    ev.put("completed", submitted);
                    ev.put("courseName", a.getCourse().getName());
                    ev.put("courseCode", a.getCourse().getCode());
                    ev.put("points", a.getPoints());
                    events.add(ev);
                }
            }
        } else if (user.getRole() == Role.TEACHER) {
            List<Assignment> assignments = assignmentRepository.findUpcomingForTeacher(user.getId(), start, end);
            for (Assignment a : assignments) {
                Map<String, Object> ev = new HashMap<>();
                ev.put("id", "assignment-" + a.getId());
                ev.put("rawId", a.getId());
                ev.put("title", a.getTitle());
                ev.put("type", "ASSIGNMENT");
                ev.put("date", a.getDueDate() != null ? a.getDueDate().toString() : null);
                ev.put("priority", a.getPriority());
                ev.put("courseName", a.getCourse().getName());
                ev.put("courseCode", a.getCourse().getCode());
                ev.put("points", a.getPoints());
                events.add(ev);
            }
        } else if (user.getRole() == Role.ADMIN) {
            List<Assignment> assignments = assignmentRepository.findAll().stream()
                    .filter(a -> a.getDueDate() != null && !a.getDueDate().isBefore(start) && !a.getDueDate().isAfter(end))
                    .toList();
            for (Assignment a : assignments) {
                Map<String, Object> ev = new HashMap<>();
                ev.put("id", "assignment-" + a.getId());
                ev.put("rawId", a.getId());
                ev.put("title", a.getTitle());
                ev.put("type", "ASSIGNMENT");
                ev.put("date", a.getDueDate().toString());
                ev.put("priority", a.getPriority());
                ev.put("courseName", a.getCourse().getName());
                ev.put("courseCode", a.getCourse().getCode());
                ev.put("points", a.getPoints());
                events.add(ev);
            }
        }

        // Sort events chronologically
        events.sort((a, b) -> {
            String da = (String) a.get("date");
            String db = (String) b.get("date");
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            return da.compareTo(db);
        });

        return events;
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
