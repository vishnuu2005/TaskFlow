package com.taskflow.service;

import com.taskflow.entity.Assignment;
import com.taskflow.entity.Enrollment;
import com.taskflow.entity.Task;
import com.taskflow.entity.User;
import com.taskflow.repository.AssignmentRepository;
import com.taskflow.repository.AssignmentSubmissionRepository;
import com.taskflow.repository.EnrollmentRepository;
import com.taskflow.repository.TaskRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class DueReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(DueReminderScheduler.class);

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TaskRepository taskRepository;
    private final NotificationService notificationService;

    public DueReminderScheduler(
            AssignmentRepository assignmentRepository,
            AssignmentSubmissionRepository submissionRepository,
            EnrollmentRepository enrollmentRepository,
            TaskRepository taskRepository,
            NotificationService notificationService) {
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.taskRepository = taskRepository;
        this.notificationService = notificationService;
    }

    // Run hourly or every 6 hours; configurable via app.scheduler.reminder-rate
    @Scheduled(fixedRateString = "${app.scheduler.reminder-rate:3600000}", initialDelay = 10000)
    @Transactional
    public void checkApproachingDueDates() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        try {
            // 1. Check assignments due tomorrow
            List<Assignment> allAssignments = assignmentRepository.findAll();
            for (Assignment a : allAssignments) {
                if (tomorrow.equals(a.getDueDate())) {
                    List<Enrollment> enrollments = enrollmentRepository.findByCourse_Id(a.getCourse().getId());
                    for (Enrollment e : enrollments) {
                        User student = e.getStudent();
                        boolean submitted = submissionRepository.existsByAssignment_IdAndStudent_Id(a.getId(), student.getId());
                        if (!submitted) {
                            notificationService.createNotification(
                                    student,
                                    "⏰ Due Tomorrow: " + a.getTitle(),
                                    "Assignment in " + a.getCourse().getCode() + " is due tomorrow: " + a.getTitle(),
                                    "DUE_DATE",
                                    "/assignments/" + a.getId()
                            );
                        }
                    }
                }
            }

            // 2. Check personal tasks due tomorrow
            List<Task> allTasks = taskRepository.findAll();
            for (Task t : allTasks) {
                if (!t.isCompleted() && tomorrow.equals(t.getDueDate())) {
                    notificationService.createNotification(
                            t.getUser(),
                            "⏰ Task Due Tomorrow: " + t.getTitle(),
                            "Don't forget to complete your task: " + t.getTitle(),
                            "DUE_DATE",
                            "/tasks"
                    );
                }
            }
        } catch (Exception ex) {
            log.warn("Scheduler checkApproachingDueDates encountered an error: {}", ex.getMessage());
        }
    }
}
