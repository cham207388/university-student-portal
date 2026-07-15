package com.abc.studentportal.enrollment.application;

import com.abc.studentportal.common.exception.ConflictException;
import com.abc.studentportal.common.exception.ResourceNotFoundException;
import com.abc.studentportal.course.application.CourseRepository;
import com.abc.studentportal.course.domain.CourseStatus;
import com.abc.studentportal.course.persistence.postgres.CourseEntity;
import com.abc.studentportal.course.persistence.postgres.CourseJpaRepository;
import com.abc.studentportal.enrollment.domain.Enrollment;
import com.abc.studentportal.enrollment.domain.EnrollmentStatus;
import com.abc.studentportal.enrollment.persistence.postgres.EnrollmentEntity;
import com.abc.studentportal.enrollment.persistence.postgres.EnrollmentJpaRepository;
import com.abc.studentportal.student.application.StudentRepository;
import com.abc.studentportal.student.domain.StudentStatus;
import com.abc.studentportal.student.persistence.postgres.StudentEntity;
import com.abc.studentportal.student.persistence.postgres.StudentJpaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * PostgreSQL enrollment facade.  The complete enrollment operation runs in one
 * database transaction so relationship checks and the unique active-enrollment
 * constraint are evaluated atomically.
 */
@Service
@Profile({"local-postgres", "test-postgres"})
@Transactional
public class PostgresEnrollmentService extends EnrollmentService {

    private final EnrollmentJpaRepository enrollmentEntities;

    private final StudentJpaRepository studentEntities;

    private final CourseJpaRepository courseEntities;

    private final Clock clock;

    public PostgresEnrollmentService(EnrollmentRepository enrollments,
                                     StudentRepository students,
                                     CourseRepository courses,
                                     Clock clock,
                                     EnrollmentJpaRepository enrollmentEntities,
                                     StudentJpaRepository studentEntities,
                                     CourseJpaRepository courseEntities) {

        super(enrollments, students, courses, clock);
        this.enrollmentEntities = enrollmentEntities;
        this.studentEntities = studentEntities;
        this.courseEntities = courseEntities;
        this.clock = clock;
    }

    @Override
    public Enrollment enroll(UUID studentId, UUID courseId) {

        CourseEntity course = courseEntities.findLockedById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
        StudentEntity student = studentEntities.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
        if (student.getStatus() != StudentStatus.ACTIVE)
            throw new ConflictException("Student is not active and cannot enroll");
        if (course.getStatus() != CourseStatus.OPEN) throw new ConflictException("Course is not open for enrollment");
        if (enrollmentEntities.existsByStudent_IdAndCourse_IdAndStatusIn(studentId, courseId,
                Set.of(EnrollmentStatus.ENROLLED, EnrollmentStatus.WAITLISTED))) {
            throw new ConflictException("Student already has an active enrollment for this course");
        }
        course.reserveSeat();
        Instant now = clock.instant();
        course.touch(now);
        return domain(enrollmentEntities.saveAndFlush(new EnrollmentEntity(UUID.randomUUID(), student, course,
                EnrollmentStatus.ENROLLED, now, null, null, now, now, 0)));
    }

    @Override
    public Enrollment changeStatus(UUID id, EnrollmentStatus target, String finalGrade, long version) {

        EnrollmentEntity snapshot = enrollmentEntities.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", id));
        CourseEntity course = courseEntities.findLockedById(snapshot.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", snapshot.getCourseId()));
        EnrollmentEntity current = enrollmentEntities.findLockedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", id));
        if (current.getVersion() != version)
            throw new ObjectOptimisticLockingFailureException(EnrollmentEntity.class, id);
        Enrollment changed = domain(current).transitionTo(target, finalGrade, clock.instant());
        if (!consumesCapacity(current.getStatus()) && changed.consumesCapacity()) course.reserveSeat();
        if (consumesCapacity(current.getStatus()) && !changed.consumesCapacity()) course.releaseSeat();
        current.transition(changed.status(), changed.droppedAt(), changed.finalGrade());
        current.touch(changed.updatedAt());
        course.touch(changed.updatedAt());
        return domain(enrollmentEntities.saveAndFlush(current));
    }

    @Override
    public Enrollment drop(UUID id, long version) {

        return changeStatus(id, EnrollmentStatus.DROPPED, null, version);
    }

    @Override
    @Transactional(readOnly = true)
    public Enrollment get(UUID id) {

        return enrollmentEntities.findById(id).map(this::domain)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", id));
    }

    private Enrollment domain(EnrollmentEntity e) {

        return new Enrollment(e.getId(), e.getStudentId(), e.getCourseId(), e.getStatus(), e.getEnrolledAt(), e.getDroppedAt(),
                e.getFinalGrade(), e.getCreatedAt(), e.getUpdatedAt(), e.getVersion());
    }

    private boolean consumesCapacity(EnrollmentStatus status) {

        return status == EnrollmentStatus.ENROLLED || status == EnrollmentStatus.COMPLETED;
    }

}
