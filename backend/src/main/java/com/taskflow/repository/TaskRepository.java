package com.taskflow.repository;

import com.taskflow.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUser_Id(Long userId);

    Optional<Task> findByIdAndUser_Id(Long taskId, Long userId);

    long countByUser_Id(Long userId);

    long countByUser_IdAndCompletedTrue(Long userId);
}