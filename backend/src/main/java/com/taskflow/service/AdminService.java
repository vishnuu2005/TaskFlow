package com.taskflow.service;

import com.taskflow.dto.UserSummaryResponse;
import com.taskflow.entity.Role;
import com.taskflow.entity.User;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public AdminService(UserRepository userRepository, TaskRepository taskRepository) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listUsers(String roleFilter, String query) {
        Role role = null;
        if (roleFilter != null && !roleFilter.isBlank() && !"ALL".equalsIgnoreCase(roleFilter)) {
            try {
                role = Role.valueOf(roleFilter.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }

        String search = (query != null && !query.isBlank()) ? query.trim() : null;

        return userRepository.searchUsers(role, search).stream()
                .map(UserSummaryResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return UserSummaryResponse.fromEntity(user);
    }

    @Transactional
    public UserSummaryResponse updateUserRole(Long id, String roleStr) {
        if (roleStr == null || roleStr.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role is required");
        }

        Role newRole;
        try {
            newRole = Role.valueOf(roleStr.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + roleStr);
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setRole(newRole);
        User saved = userRepository.save(user);
        return UserSummaryResponse.fromEntity(saved);
    }

    @Transactional
    public UserSummaryResponse updateUser(Long id, com.taskflow.dto.AdminUserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!user.getEmail().equalsIgnoreCase(request.email())) {
            if (userRepository.existsByEmail(request.email().trim())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already taken");
            }
            user.setEmail(request.email().trim());
        }

        user.setName(request.name().trim());

        if (request.role() != null && !request.role().isBlank()) {
            try {
                user.setRole(Role.valueOf(request.role().trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + request.role());
            }
        }

        if (request.active() != null) {
            user.setActive(request.active());
        }

        User saved = userRepository.save(user);
        return UserSummaryResponse.fromEntity(saved);
    }

    @Transactional
    public UserSummaryResponse updateUserStatus(Long id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setActive(active);
        User saved = userRepository.save(user);
        return UserSummaryResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPlatformStats() {
        long totalUsers = userRepository.count();
        long adminCount = userRepository.countByRole(Role.ADMIN);
        long teacherCount = userRepository.countByRole(Role.TEACHER);
        long studentCount = userRepository.countByRole(Role.STUDENT);
        long activeCount = userRepository.countByActiveTrue();
        long inactiveCount = userRepository.countByActiveFalse();
        long totalTasks = taskRepository.count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("admins", adminCount);
        stats.put("teachers", teacherCount);
        stats.put("students", studentCount);
        stats.put("activeUsers", activeCount);
        stats.put("inactiveUsers", inactiveCount);
        stats.put("totalPersonalTasks", totalTasks);

        return stats;
    }
}
