package com.abc.studentportal.enrollment.persistence.postgres;

import com.abc.studentportal.common.persistence.postgres.BaseEntity;
import com.abc.studentportal.course.persistence.postgres.CourseEntity;
import com.abc.studentportal.enrollment.domain.EnrollmentStatus;
import com.abc.studentportal.student.persistence.postgres.StudentEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Table(name = "enrollments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EnrollmentEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEntity student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private CourseEntity course;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status;

    @Column(name = "enrolled_at", nullable = false)
    private Instant enrolledAt;

    @Column(name = "dropped_at")
    private Instant droppedAt;

    @Column(name = "final_grade")
    private String finalGrade;

    @Version
    @Column(nullable = false)
    private long version;

    public EnrollmentEntity(UUID id, StudentEntity student, CourseEntity course, EnrollmentStatus status, Instant enrolledAt, Instant droppedAt, String finalGrade) {

        this(id, student, course, status, enrolledAt, droppedAt, finalGrade, enrolledAt, enrolledAt, 0);
    }

    public EnrollmentEntity(UUID id, StudentEntity student, CourseEntity course, EnrollmentStatus status, Instant enrolledAt,
                            Instant droppedAt, String finalGrade, Instant createdAt, Instant updatedAt, long version) {

        this.id = id;
        this.student = student;
        this.course = course;
        this.status = status;
        this.enrolledAt = enrolledAt;
        this.droppedAt = droppedAt;
        this.finalGrade = finalGrade;
        this.version = version;
        audit(createdAt, updatedAt);
    }

    public UUID getStudentId() {

        return student.getId();
    }

    public UUID getCourseId() {

        return course.getId();
    }

    public void transition(EnrollmentStatus status, Instant droppedAt, String finalGrade) {

        this.status = status;
        this.droppedAt = droppedAt;
        this.finalGrade = finalGrade;
    }

}
