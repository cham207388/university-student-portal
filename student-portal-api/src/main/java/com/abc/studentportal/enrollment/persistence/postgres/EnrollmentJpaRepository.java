package com.abc.studentportal.enrollment.persistence.postgres;

import com.abc.studentportal.enrollment.domain.EnrollmentStatus;
import com.abc.studentportal.student.persistence.postgres.StudentEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnrollmentJpaRepository extends JpaRepository<EnrollmentEntity, UUID> {

    List<EnrollmentEntity> findByStudent_Id(UUID id);

    List<EnrollmentEntity> findByCourse_Id(UUID id);

    boolean existsByStudent_Id(UUID id);

    boolean existsByCourse_Id(UUID id);

    boolean existsByStudent_IdAndCourse_IdAndStatusIn(UUID studentId, UUID courseId, Collection<EnrollmentStatus> statuses);

    Page<EnrollmentEntity> findAll(Pageable pageable);

    Page<EnrollmentEntity> findByStatus(EnrollmentStatus status, Pageable pageable);

    Page<EnrollmentEntity> findByStudent_Id(UUID id, Pageable pageable);

    Page<EnrollmentEntity> findByStudent_IdAndEnrolledAtGreaterThanEqual(UUID id, Instant from, Pageable pageable);

    Page<EnrollmentEntity> findByStudent_IdAndEnrolledAtLessThanEqual(UUID id, Instant to, Pageable pageable);

    Page<EnrollmentEntity> findByStudent_IdAndEnrolledAtBetween(UUID id, Instant from, Instant to, Pageable pageable);

    Page<EnrollmentEntity> findByCourse_Id(UUID id, Pageable pageable);

    Page<EnrollmentEntity> findByCourse_IdAndEnrolledAtGreaterThanEqual(UUID id, Instant from, Pageable pageable);

    Page<EnrollmentEntity> findByCourse_IdAndEnrolledAtLessThanEqual(UUID id, Instant to, Pageable pageable);

    Page<EnrollmentEntity> findByCourse_IdAndEnrolledAtBetween(UUID id, Instant from, Instant to, Pageable pageable);

    @Query("select e from EnrollmentEntity e where e.student.id = :id and (:from is null or e.enrolledAt >= :from) and (:to is null or e.enrolledAt <= :to)")
    Page<EnrollmentEntity> findByStudent(@Param("id") UUID id, @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    @Query("select e from EnrollmentEntity e where e.course.id = :id and (:from is null or e.enrolledAt >= :from) and (:to is null or e.enrolledAt <= :to)")
    Page<EnrollmentEntity> findByCourse(@Param("id") UUID id, @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    @Query(value = "select distinct s from StudentEntity s join EnrollmentEntity e on e.student.id = s.id where e.course.id = :courseId",
            countQuery = "select count(distinct s.id) from StudentEntity s join EnrollmentEntity e on e.student.id = s.id where e.course.id = :courseId")
    Page<StudentEntity> findDistinctStudentsByCourse(@Param("courseId") UUID courseId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EnrollmentEntity e where e.id = :id")
    Optional<EnrollmentEntity> findLockedById(@Param("id") UUID id);

}
