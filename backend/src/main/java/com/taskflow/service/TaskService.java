package com.taskflow.service;

import com.taskflow.dto.SubTaskDto;
import com.taskflow.dto.TaskRequest;
import com.taskflow.dto.TaskResponse;
import com.taskflow.entity.SubTask;
import com.taskflow.entity.Task;
import com.taskflow.entity.User;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
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
        task.setPriority(request.priority() == null ? "MEDIUM" : request.priority());
        task.setDueDate(request.dueDate());
        task.setCategory(request.category());
        if (request.status() != null && !request.status().isBlank()) {
            task.setStatus(request.status());
        } else {
            task.setCompleted(false);
        }
        task.setUser(user);

        if (request.subtasks() != null) {
            for (SubTaskDto stDto : request.subtasks()) {
                if (stDto.title() != null && !stDto.title().isBlank()) {
                    task.addSubTask(new SubTask(stDto.title().trim(), stDto.completed(), task));
                }
            }
        }

        Task savedTask = taskRepository.save(task);
        return toResponse(savedTask);
    }

    // 2. GET ALL TASKS (Supports filtering, search, sorting)
    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasks() {
        User user = getCurrentUser();
        return taskRepository.findByUser_Id(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> filterTasks(String search, Boolean completed, String priority, String category, String sort) {
        User user = getCurrentUser();
        List<Task> tasks = taskRepository.filterTasks(user.getId(), search, completed, priority, category);

        // Sorting
        if ("due".equalsIgnoreCase(sort)) {
            tasks = tasks.stream().sorted((a, b) -> {
                if (a.getDueDate() == null && b.getDueDate() == null) return 0;
                if (a.getDueDate() == null) return 1;
                if (b.getDueDate() == null) return -1;
                return a.getDueDate().compareTo(b.getDueDate());
            }).toList();
        } else if ("priority".equalsIgnoreCase(sort)) {
            Map<String, Integer> pMap = Map.of("HIGH", 1, "MEDIUM", 2, "LOW", 3);
            tasks = tasks.stream().sorted((a, b) ->
                pMap.getOrDefault(a.getPriority(), 2).compareTo(pMap.getOrDefault(b.getPriority(), 2))
            ).toList();
        } else if ("oldest".equalsIgnoreCase(sort)) {
            tasks = tasks.stream().sorted((a, b) -> a.getId().compareTo(b.getId())).toList();
        } else {
            // Newest default
            tasks = tasks.stream().sorted((a, b) -> b.getId().compareTo(a.getId())).toList();
        }

        return tasks.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> searchTasksPaginated(
            String search, Boolean completed, String priority, String category,
            int page, int size, String sortBy, String sortDir) {
        User user = getCurrentUser();
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProperty = "dueDate".equalsIgnoreCase(sortBy) ? "dueDate" : "createdAt";
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortProperty));

        return taskRepository.searchTasks(user.getId(), search, completed, priority, category, pageable)
                .map(this::toResponse);
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
    public TaskResponse updateTask(Long taskId, TaskRequest request) {
        User user = getCurrentUser();
        Task task = findOwnedTask(taskId, user.getId());

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(request.priority() == null ? "MEDIUM" : request.priority());
        task.setDueDate(request.dueDate());
        if (request.category() != null) {
            task.setCategory(request.category());
        }
        if (request.status() != null && !request.status().isBlank()) {
            task.setStatus(request.status());
        }

        if (request.subtasks() != null) {
            List<SubTask> updated = new ArrayList<>();
            for (SubTaskDto stDto : request.subtasks()) {
                if (stDto.title() != null && !stDto.title().isBlank()) {
                    updated.add(new SubTask(stDto.title().trim(), stDto.completed(), task));
                }
            }
            task.setSubtasks(updated);
        }

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

    // 7. TOGGLE SUBTASK COMPLETION
    @Transactional
    public TaskResponse toggleSubTask(Long taskId, Long subTaskId) {
        User user = getCurrentUser();
        Task task = findOwnedTask(taskId, user.getId());

        for (SubTask st : task.getSubtasks()) {
            if (st.getId().equals(subTaskId)) {
                st.setCompleted(!st.isCompleted());
                break;
            }
        }

        Task updatedTask = taskRepository.save(task);
        return toResponse(updatedTask);
    }

    // 8. DELETE TASK
    @Transactional
    public void deleteTask(Long taskId) {
        User user = getCurrentUser();
        Task task = findOwnedTask(taskId, user.getId());
        taskRepository.delete(task);
    }

    // 9. GET STATS
    @Transactional(readOnly = true)
    public Map<String, Object> getStats() {
        User user = getCurrentUser();
        long total = taskRepository.countByUser_Id(user.getId());
        long completed = taskRepository.countByUser_IdAndCompletedTrue(user.getId());
        long pending = total - completed;
        LocalDate today = LocalDate.now();
        long overdue = taskRepository.countByUser_IdAndCompletedFalseAndDueDateBefore(user.getId(), today);
        long highPriority = taskRepository.countByUser_IdAndCompletedFalseAndPriority(user.getId(), "HIGH");

        return Map.of(
                "total", total,
                "completed", completed,
                "pending", pending,
                "overdue", overdue,
                "highPriority", highPriority,
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
        List<SubTaskDto> subTaskDtos = task.getSubtasks() != null
                ? task.getSubtasks().stream()
                    .map(st -> new SubTaskDto(st.getId(), st.getTitle(), st.isCompleted()))
                    .toList()
                : List.of();

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.isCompleted(),
                task.getPriority(),
                task.getDueDate(),
                task.getCategory(),
                task.getStatus(),
                task.getCreatedAt(),
                subTaskDtos
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
