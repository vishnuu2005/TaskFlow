package com.taskflow.repository;

import com.taskflow.entity.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {

    List<AssignmentSubmission> findByAssignment_Id(Long assignmentId);

    List<AssignmentSubmission> findByAssignment_Course_Id(Long courseId);

    List<AssignmentSubmission> findByStudent_Id(Long studentId);

    Optional<AssignmentSubmission> findByAssignment_IdAndStudent_Id(Long assignmentId, Long studentId);

    boolean existsByAssignment_IdAndStudent_Id(Long assignmentId, Long studentId);

    long countByAssignment_Id(Long assignmentId);

    long countByAssignment_Course_Teacher_Id(Long teacherId);

    long countByAssignment_Course_Teacher_IdAndStatus(Long teacherId, String status);

    long countByStudent_Id(Long studentId);

    long countByStudent_IdAndStatus(Long studentId, String status);

    @Query("SELECT s FROM AssignmentSubmission s WHERE s.assignment.course.teacher.id = :teacherId AND s.status = 'SUBMITTED' ORDER BY s.submittedAt ASC")
    List<AssignmentSubmission> findPendingSubmissionsForTeacher(@Param("teacherId") Long teacherId);
}
