package com.taskflow.repository;

import com.taskflow.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByTeacher_Id(Long teacherId);

    List<Course> findByActiveTrue();

    Optional<Course> findByCode(String code);

    boolean existsByCode(String code);

    long countByTeacher_Id(Long teacherId);

    @Query("SELECT c FROM Course c WHERE " +
           "(:query IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.code) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Course> searchCourses(@Param("query") String query);
}
