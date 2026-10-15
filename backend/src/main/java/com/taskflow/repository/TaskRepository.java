package com.taskflow.repository;

import com.taskflow.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUser_Id(Long userId);

    List<Task> findByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<Task> findByIdAndUser_Id(Long id, Long userId);

    long countByUser_Id(Long userId);

    long countByUser_IdAndCompletedTrue(Long userId);

    long countByUser_IdAndCompletedFalse(Long userId);

    long countByUser_IdAndCompletedFalseAndDueDateBefore(Long userId, LocalDate date);

    long countByUser_IdAndCompletedFalseAndPriority(Long userId, String priority);

    List<Task> findByUser_IdAndDueDateBetween(Long userId, LocalDate start, LocalDate end);

    @Query("SELECT t FROM Task t WHERE t.user.id = :userId AND " +
           "(:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:completed IS NULL OR t.completed = :completed) AND " +
           "(:priority IS NULL OR t.priority = :priority) AND " +
           "(:category IS NULL OR LOWER(t.category) = LOWER(:category))")
    Page<Task> searchTasks(
            @Param("userId") Long userId,
            @Param("search") String search,
            @Param("completed") Boolean completed,
            @Param("priority") String priority,
            @Param("category") String category,
            Pageable pageable
    );

    @Query("SELECT t FROM Task t WHERE t.user.id = :userId AND " +
           "(:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:completed IS NULL OR t.completed = :completed) AND " +
           "(:priority IS NULL OR t.priority = :priority) AND " +
           "(:category IS NULL OR LOWER(t.category) = LOWER(:category))")
    List<Task> filterTasks(
            @Param("userId") Long userId,
            @Param("search") String search,
            @Param("completed") Boolean completed,
            @Param("priority") String priority,
            @Param("category") String category
    );
}