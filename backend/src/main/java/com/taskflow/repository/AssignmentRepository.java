package com.taskflow.repository;

import com.taskflow.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByCourse_Id(Long courseId);

    List<Assignment> findByCourse_IdOrderByDueDateAsc(Long courseId);

    List<Assignment> findByCourse_IdIn(List<Long> courseIds);

    List<Assignment> findByCourse_IdInOrderByDueDateAsc(List<Long> courseIds);

    List<Assignment> findByCourse_Teacher_Id(Long teacherId);

    List<Assignment> findByCourse_Teacher_IdOrderByDueDateAsc(Long teacherId);

    long countByCourse_Id(Long courseId);

    long countByCourse_Teacher_Id(Long teacherId);

    @Query("SELECT a FROM Assignment a WHERE a.course.id IN :courseIds AND a.dueDate BETWEEN :startDate AND :endDate")
    List<Assignment> findUpcomingForCourses(
            @Param("courseIds") List<Long> courseIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT a FROM Assignment a WHERE a.course.teacher.id = :teacherId AND a.dueDate BETWEEN :startDate AND :endDate")
    List<Assignment> findUpcomingForTeacher(
            @Param("teacherId") Long teacherId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
