package com.abc.studentportal.enrollment.persistence.postgres;

import com.abc.studentportal.course.persistence.postgres.CourseEntity;
import com.abc.studentportal.course.persistence.postgres.CourseJpaRepository;
import com.abc.studentportal.enrollment.application.EnrollmentRepository;
import com.abc.studentportal.enrollment.domain.Enrollment;
import com.abc.studentportal.enrollment.domain.EnrollmentStatus;
import com.abc.studentportal.student.persistence.postgres.StudentJpaRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@Primary
@Profile({"local-postgres", "test-postgres", "migration"})
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

        CourseEntity course = courses.findLockedById(enrollment.courseId()).orElseThrow();
        if (enrollment.consumesCapacity()) course.reserveSeat();
        return toDomain(repo.saveAndFlush(new EnrollmentEntity(enrollment.id(),
                students.getReferenceById(enrollment.studentId()), course, enrollment.status(), enrollment.enrolledAt(),
                enrollment.droppedAt(), enrollment.finalGrade(), enrollment.createdAt(), enrollment.updatedAt(), enrollment.version())));
    }

    public Enrollment update(Enrollment enrollment) {

        EnrollmentEntity existing = repo.findById(enrollment.id()).orElseThrow();
        if (existing.getVersion() != enrollment.version())
            throw new ObjectOptimisticLockingFailureException(EnrollmentEntity.class, enrollment.id());
        existing.transition(enrollment.status(), enrollment.droppedAt(), enrollment.finalGrade());
        existing.touch(enrollment.updatedAt());
        return toDomain(repo.saveAndFlush(existing));
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

        return new EnrollmentEntity(enrollment.id(), students.getReferenceById(enrollment.studentId()),
                courses.getReferenceById(enrollment.courseId()), enrollment.status(), enrollment.enrolledAt(),
                enrollment.droppedAt(), enrollment.finalGrade(), enrollment.createdAt(), enrollment.updatedAt(), enrollment.version());
    }

    private Enrollment toDomain(EnrollmentEntity enrollmentEntity) {

        return new Enrollment(enrollmentEntity.getId(), enrollmentEntity.getStudentId(), enrollmentEntity.getCourseId(), enrollmentEntity.getStatus(), enrollmentEntity.getEnrolledAt(), enrollmentEntity.getDroppedAt(), enrollmentEntity.getFinalGrade(), enrollmentEntity.getCreatedAt(), enrollmentEntity.getUpdatedAt(), enrollmentEntity.getVersion());
    }

}
