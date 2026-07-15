package com.abc.studentportal.postgres.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.abc.studentportal.postgres.entity.CourseEntity;

import java.util.Optional;
import java.util.UUID;

public interface CourseJpaRepository extends JpaRepository<CourseEntity, UUID> {

    Optional<CourseEntity> findByCourseCode(String courseCode);

    @Query("select course from CourseEntity course where course.courseCode = :courseCode")
    Optional<CourseEntity> findByCode(@Param("courseCode") String courseCode);

}
