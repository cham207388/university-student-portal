package com.abc.studentportal.enrollment.persistence.postgres;

import com.abc.studentportal.enrollment.application.EnrollmentRepository;
import com.abc.studentportal.enrollment.domain.*;
import com.abc.studentportal.postgres.entity.*;
import com.abc.studentportal.postgres.repository.*;

import java.util.*;

import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile({"local-postgres", "test-postgres"})
@Transactional
public class PostgresEnrollmentRepository implements EnrollmentRepository {

    private final EnrollmentJpaRepository repo;

    private final StudentJpaRepository students;

    private final CourseJpaRepository courses;

    public PostgresEnrollmentRepository(EnrollmentJpaRepository repo, StudentJpaRepository students, CourseJpaRepository courses) {
        this.repo = repo;
        this.students = students;
        this.courses = courses;
    }

    public Enrollment create(Enrollment enrollment) {
        return toDomain(repo.save(toEntity(enrollment)));
    }

    public Enrollment update(Enrollment enrollment) {
        return toDomain(repo.save(toEntity(enrollment)));
    }

    public Optional<Enrollment> findById(UUID id) {
        return repo.findById(id).map(this::toDomain);
    }

    public boolean existsActiveByStudentIdAndCourseId(UUID studentId, UUID courseId) {
        return repo.existsByStudent_IdAndCourse_IdAndStatusIn(studentId, courseId, Set.of(EnrollmentStatus.ENROLLED, EnrollmentStatus.WAITLISTED));
    }

    public boolean existsByStudentId(UUID id) {
        return repo.existsByStudent_Id(id);
    }

    public boolean existsByCourseId(UUID id) {
        return repo.existsByCourse_Id(id);
    }

    private EnrollmentEntity toEntity(Enrollment enrollment) {
        return new EnrollmentEntity(enrollment.id(), students.getReferenceById(enrollment.studentId()), courses.getReferenceById(enrollment.courseId()), enrollment.status(), enrollment.enrolledAt(), enrollment.droppedAt(), enrollment.finalGrade(), enrollment.createdAt(), enrollment.updatedAt(), enrollment.version());
    }

    private Enrollment toDomain(EnrollmentEntity enrollmentEntity) {
        return new Enrollment(enrollmentEntity.getId(), enrollmentEntity.getStudentId(), enrollmentEntity.getCourseId(), enrollmentEntity.getStatus(), enrollmentEntity.getEnrolledAt(), enrollmentEntity.getDroppedAt(), enrollmentEntity.getFinalGrade(), enrollmentEntity.getCreatedAt(), enrollmentEntity.getUpdatedAt(), enrollmentEntity.getVersion());
    }

}
