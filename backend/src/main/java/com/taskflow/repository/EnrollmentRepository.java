package com.taskflow.repository;

import com.taskflow.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByCourse_Id(Long courseId);

    List<Enrollment> findByStudent_Id(Long studentId);

    Optional<Enrollment> findByCourse_IdAndStudent_Id(Long courseId, Long studentId);

    boolean existsByCourse_IdAndStudent_Id(Long courseId, Long studentId);

    void deleteByCourse_IdAndStudent_Id(Long courseId, Long studentId);

    long countByCourse_Id(Long courseId);

    long countByStudent_Id(Long studentId);

    long countByCourse_Teacher_Id(Long teacherId);
}
