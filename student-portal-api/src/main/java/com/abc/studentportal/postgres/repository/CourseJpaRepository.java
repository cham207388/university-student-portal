package com.abc.studentportal.postgres.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.abc.studentportal.postgres.entity.CourseEntity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.abc.studentportal.course.domain.CourseStatus;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface CourseJpaRepository extends JpaRepository<CourseEntity, UUID> {

    boolean existsByDepartment_Id(UUID departmentId);
    boolean existsByInstructor_Id(UUID instructorId);

    Optional<CourseEntity> findByCourseCode(String courseCode);

    @Query("select course from CourseEntity course where course.courseCode = :courseCode")
    Optional<CourseEntity> findByCode(@Param("courseCode") String courseCode);
    Page<CourseEntity> findAll(Pageable pageable);
    Page<CourseEntity> findByDepartment_Id(UUID departmentId, Pageable pageable);
    Page<CourseEntity> findByInstructor_Id(UUID instructorId, Pageable pageable);
    Page<CourseEntity> findByStatus(CourseStatus status, Pageable pageable);

    @Query(value = "select distinct c from CourseEntity c join EnrollmentEntity e on e.course.id = c.id where e.student.id = :studentId",
            countQuery = "select count(distinct c.id) from CourseEntity c join EnrollmentEntity e on e.course.id = c.id where e.student.id = :studentId")
    Page<CourseEntity> findDistinctByStudent(@Param("studentId") UUID studentId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CourseEntity c where c.id = :id")
    Optional<CourseEntity> findLockedById(@Param("id") UUID id);

}
