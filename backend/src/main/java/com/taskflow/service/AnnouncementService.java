package com.taskflow.service;

import com.taskflow.dto.AnnouncementRequest;
import com.taskflow.dto.AnnouncementResponse;
import com.taskflow.entity.Announcement;
import com.taskflow.entity.Course;
import com.taskflow.entity.Enrollment;
import com.taskflow.entity.Role;
import com.taskflow.entity.User;
import com.taskflow.repository.AnnouncementRepository;
import com.taskflow.repository.CourseRepository;
import com.taskflow.repository.EnrollmentRepository;
import com.taskflow.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public AnnouncementService(
            AnnouncementRepository announcementRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.announcementRepository = announcementRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public AnnouncementResponse createAnnouncement(AnnouncementRequest request) {
        User author = getCurrentUser();

        Course course = null;
        if (request.courseId() != null) {
            course = courseRepository.findById(request.courseId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

            if (author.getRole() != Role.ADMIN && !course.getTeacher().getId().equals(author.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Teachers can only post announcements for their own courses");
            }
        } else {
            // Platform-wide announcement: Admin only
            if (author.getRole() != Role.ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can publish platform-wide announcements");
            }
        }

        Announcement announcement = new Announcement(
                request.title().trim(),
                request.content().trim(),
                course,
                author,
                request.priority() != null ? request.priority() : "NORMAL"
        );

        Announcement saved = announcementRepository.save(announcement);

        // Send in-app notifications
        if (course != null) {
            List<Enrollment> enrollments = enrollmentRepository.findByCourse_Id(course.getId());
            List<User> students = enrollments.stream().map(Enrollment::getStudent).toList();
            notificationService.notifyUsers(
                    students,
                    "Class Announcement: " + saved.getTitle(),
                    "New announcement in " + course.getName() + ": " + saved.getTitle(),
                    "ANNOUNCEMENT",
                    "/announcements"
            );
        } else {
            // Platform announcement: notify all users
            List<User> allUsers = userRepository.findAll().stream()
                    .filter(u -> !u.getId().equals(author.getId()) && u.isActive())
                    .toList();
            notificationService.notifyUsers(
                    allUsers,
                    "📢 " + saved.getTitle(),
                    saved.getContent().length() > 100 ? saved.getContent().substring(0, 97) + "..." : saved.getContent(),
                    "ANNOUNCEMENT",
                    "/announcements"
            );
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getAnnouncements(Long courseId) {
        User user = getCurrentUser();

        if (courseId != null) {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

            if (user.getRole() == Role.STUDENT) {
                if (!enrollmentRepository.existsByCourse_IdAndStudent_Id(courseId, user.getId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not enrolled in this course");
                }
            } else if (user.getRole() == Role.TEACHER) {
                if (!course.getTeacher().getId().equals(user.getId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your course");
                }
            }

            return announcementRepository.findByCourse_IdOrderByCreatedAtDesc(courseId).stream()
                    .map(this::toResponse)
                    .toList();
        }

        // Global list
        if (user.getRole() == Role.ADMIN) {
            return announcementRepository.findAllOrdered().stream()
                    .map(this::toResponse)
                    .toList();
        } else if (user.getRole() == Role.TEACHER) {
            return announcementRepository.findByCourse_Teacher_IdOrCourseIsNullOrderByCreatedAtDesc(user.getId()).stream()
                    .map(this::toResponse)
                    .toList();
        } else {
            // Student: platform announcements + enrolled course announcements
            List<Long> enrolledCourseIds = enrollmentRepository.findByStudent_Id(user.getId()).stream()
                    .map(e -> e.getCourse().getId())
                    .toList();

            if (enrolledCourseIds.isEmpty()) {
                return announcementRepository.findByCourseIsNullOrderByCreatedAtDesc().stream()
                        .map(this::toResponse)
                        .toList();
            }

            return announcementRepository.findByCourse_IdInOrCourseIsNullOrderByCreatedAtDesc(enrolledCourseIds).stream()
                    .map(this::toResponse)
                    .toList();
        }
    }

    @Transactional
    public void deleteAnnouncement(Long id) {
        User user = getCurrentUser();
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Announcement not found"));

        if (user.getRole() != Role.ADMIN) {
            if (announcement.getCourse() == null || !announcement.getCourse().getTeacher().getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to delete this announcement");
            }
        }

        announcementRepository.delete(announcement);
    }

    private AnnouncementResponse toResponse(Announcement a) {
        return new AnnouncementResponse(
                a.getId(),
                a.getTitle(),
                a.getContent(),
                a.getCourse() != null ? a.getCourse().getId() : null,
                a.getCourse() != null ? a.getCourse().getName() : null,
                a.getCourse() != null ? a.getCourse().getCode() : null,
                a.getAuthor().getId(),
                a.getAuthor().getName(),
                a.getAuthor().getRole().name(),
                a.getPriority(),
                a.getCreatedAt() != null ? a.getCreatedAt().toString() : ""
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
