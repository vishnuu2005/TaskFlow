package com.taskflow.service;

import com.taskflow.dto.AdminUserUpdateRequest;
import com.taskflow.dto.UserSummaryResponse;
import com.taskflow.entity.Role;
import com.taskflow.entity.User;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private AdminService adminService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("John Doe");
        user.setEmail("john@example.com");
        user.setRole(Role.STUDENT);
        user.setActive(true);
    }

    @Test
    void listUsers_ShouldReturnMappedUsers() {
        when(userRepository.searchUsers(Role.STUDENT, "john")).thenReturn(List.of(user));

        List<UserSummaryResponse> list = adminService.listUsers("STUDENT", "john");

        assertEquals(1, list.size());
        assertEquals("John Doe", list.get(0).name());
        assertEquals("john@example.com", list.get(0).email());
        assertEquals("STUDENT", list.get(0).role());
    }

    @Test
    void updateUserRole_ShouldChangeRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserSummaryResponse updated = adminService.updateUserRole(1L, "TEACHER");

        assertEquals("TEACHER", updated.role());
        assertEquals(Role.TEACHER, user.getRole());
    }

    @Test
    void updateUserRole_InvalidRoleShouldThrow() {
        assertThrows(ResponseStatusException.class, () -> adminService.updateUserRole(1L, "SUPERHERO"));
    }

    @Test
    void updateUserStatus_ShouldDeactivateAccount() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserSummaryResponse updated = adminService.updateUserStatus(1L, false);

        assertFalse(updated.active());
        assertFalse(user.isActive());
    }

    @Test
    void updateUser_ShouldUpdateDetails() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUserUpdateRequest req = new AdminUserUpdateRequest("Johnathan Doe", "john@example.com", "ADMIN", true);
        UserSummaryResponse resp = adminService.updateUser(1L, req);

        assertEquals("Johnathan Doe", resp.name());
        assertEquals("ADMIN", resp.role());
    }

    @Test
    void getPlatformStats_ShouldReturnMetrics() {
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);
        when(userRepository.countByRole(Role.TEACHER)).thenReturn(3L);
        when(userRepository.countByRole(Role.STUDENT)).thenReturn(6L);
        when(userRepository.countByActiveTrue()).thenReturn(9L);
        when(userRepository.countByActiveFalse()).thenReturn(1L);
        when(taskRepository.count()).thenReturn(42L);

        Map<String, Object> stats = adminService.getPlatformStats();

        assertEquals(10L, stats.get("totalUsers"));
        assertEquals(1L, stats.get("admins"));
        assertEquals(3L, stats.get("teachers"));
        assertEquals(6L, stats.get("students"));
        assertEquals(42L, stats.get("totalPersonalTasks"));
    }
}
