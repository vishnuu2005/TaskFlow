
package com.taskflow.service;

import com.taskflow.dto.TaskRequest;
import com.taskflow.dto.TaskResponse;
import com.taskflow.entity.Task;
import com.taskflow.entity.User;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(
            TaskRepository taskRepository,
            UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    // 1. CREATE TASK
    @Transactional
    public TaskResponse createTask(TaskRequest request) {

        User user = getCurrentUser();

        Task task = new Task();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(
                request.priority() == null
                        ? "MEDIUM"
                        : request.priority()
        );
        task.setDueDate(request.dueDate());
        task.setCompleted(false);
        task.setUser(user);

        Task savedTask = taskRepository.save(task);

        return toResponse(savedTask);
    }

    // 2. GET ALL TASKS
    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasks() {

        User user = getCurrentUser();

        return taskRepository.findByUser_Id(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // 3. GET TASK BY ID
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long taskId) {

        User user = getCurrentUser();
        Task task = findOwnedTask(taskId, user.getId());

        return toResponse(task);
    }

    // 4. UPDATE TASK
    @Transactional
    public TaskResponse updateTask(
            Long taskId,
            TaskRequest request) {

        User user = getCurrentUser();
        Task task = findOwnedTask(taskId, user.getId());

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(
                request.priority() == null
                        ? "MEDIUM"
                        : request.priority()
        );
        task.setDueDate(request.dueDate());

        Task updatedTask = taskRepository.save(task);

        return toResponse(updatedTask);
    }

    // 5. MARK TASK AS COMPLETED
    @Transactional
    public TaskResponse completeTask(Long taskId) {

        User user = getCurrentUser();
        Task task = findOwnedTask(taskId, user.getId());

        task.setCompleted(true);

        Task updatedTask = taskRepository.save(task);

        return toResponse(updatedTask);
    }

    // 6. TOGGLE TASK COMPLETION
    @Transactional
    public TaskResponse toggleTask(Long taskId) {

        User user = getCurrentUser();
        Task task = findOwnedTask(taskId, user.getId());

        task.setCompleted(!task.isCompleted());

        Task updatedTask = taskRepository.save(task);

        return toResponse(updatedTask);
    }

    // 7. DELETE TASK
    @Transactional
    public void deleteTask(Long taskId) {

        User user = getCurrentUser();
        Task task = findOwnedTask(taskId, user.getId());

        taskRepository.delete(task);
    }

    // 8. GET STATS
    @Transactional(readOnly = true)
    public Map<String, Object> getStats() {

        User user = getCurrentUser();
        long total = taskRepository.countByUser_Id(user.getId());
        long completed = taskRepository.countByUser_IdAndCompletedTrue(user.getId());
        long pending = total - completed;

        return Map.of(
                "total", total,
                "completed", completed,
                "pending", pending,
                "completionRate", total == 0 ? 0 : (completed * 100 / total)
        );
    }

    // HELPER: FIND TASK OWNED BY CURRENT USER
    private Task findOwnedTask(Long taskId, Long userId) {

        return taskRepository
                .findByIdAndUser_Id(taskId, userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Task not found"
                        )
                );
    }

    // HELPER: CONVERT ENTITY TO RESPONSE DTO
    private TaskResponse toResponse(Task task) {

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.isCompleted(),
                task.getPriority(),
                task.getDueDate()
        );
    }

    // HELPER: GET AUTHENTICATED USER
    private User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(
                        authentication.getPrincipal())) {

            throw new AuthenticationCredentialsNotFoundException(
                    "Authentication is required"
            );
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new AuthenticationCredentialsNotFoundException(
                                "Authenticated user not found"
                        )
                );
    }
}
