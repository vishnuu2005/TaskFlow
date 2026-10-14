package com.taskflow.service;

import com.taskflow.dto.TaskRequest;
import com.taskflow.dto.TaskResponse;
import com.taskflow.entity.Task;
import com.taskflow.entity.User;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    private TaskService taskService;
    private User testUser;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, userRepository);

        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test@example.com", "credentials", java.util.Collections.emptyList())
        );

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTask_ShouldSaveAndReturnResponse() {
        TaskRequest request = new TaskRequest(
                "Complete Project",
                "Finish all tasks and test thoroughly",
                "HIGH",
                LocalDate.now().plusDays(3)
        );

        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            return t;
        });

        TaskResponse response = taskService.createTask(request);

        assertNotNull(response);
        assertEquals("Complete Project", response.title());
        assertEquals("HIGH", response.priority());
        assertFalse(response.completed());
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void getAllTasks_ShouldReturnUserTasks() {
        Task task1 = new Task();
        task1.setTitle("Task 1");
        task1.setPriority("LOW");
        task1.setUser(testUser);

        when(taskRepository.findByUser_Id(testUser.getId())).thenReturn(List.of(task1));

        List<TaskResponse> list = taskService.getAllTasks();

        assertEquals(1, list.size());
        assertEquals("Task 1", list.get(0).title());
    }

    @Test
    void toggleTask_ShouldInvertCompletionStatus() {
        Task task = new Task();
        task.setTitle("Toggleable");
        task.setCompleted(false);
        task.setUser(testUser);

        when(taskRepository.findByIdAndUser_Id(1L, testUser.getId())).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse res = taskService.toggleTask(1L);

        assertTrue(res.completed());
    }

    @Test
    void getStats_ShouldCalculateCorrectMetrics() {
        when(taskRepository.countByUser_Id(testUser.getId())).thenReturn(4L);
        when(taskRepository.countByUser_IdAndCompletedTrue(testUser.getId())).thenReturn(2L);

        Map<String, Object> stats = taskService.getStats();

        assertEquals(4L, stats.get("total"));
        assertEquals(2L, stats.get("completed"));
        assertEquals(2L, stats.get("pending"));
        assertEquals(50L, stats.get("completionRate"));
    }

    @Test
    void deleteTask_ShouldCallRepositoryDelete() {
        Task task = new Task();
        task.setTitle("To Delete");
        task.setUser(testUser);

        when(taskRepository.findByIdAndUser_Id(5L, testUser.getId())).thenReturn(Optional.of(task));

        assertDoesNotThrow(() -> taskService.deleteTask(5L));
        verify(taskRepository).delete(task);
    }

    @Test
    void findOwnedTask_ShouldThrow404WhenNotFound() {
        when(taskRepository.findByIdAndUser_Id(999L, testUser.getId())).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> taskService.getTaskById(999L));
    }
}
