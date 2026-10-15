package com.taskflow.repository;

import com.taskflow.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findByCourseIsNullOrderByCreatedAtDesc();

    List<Announcement> findByCourse_IdOrderByCreatedAtDesc(Long courseId);

    List<Announcement> findByCourse_IdInOrCourseIsNullOrderByCreatedAtDesc(List<Long> courseIds);

    List<Announcement> findByCourse_Teacher_IdOrCourseIsNullOrderByCreatedAtDesc(Long teacherId);

    long countByCourse_Teacher_Id(Long teacherId);

    @Query("SELECT a FROM Announcement a ORDER BY a.createdAt DESC")
    List<Announcement> findAllOrdered();
}
