package com.abc.studentportal.postgres.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

import com.abc.studentportal.enrollment.domain.EnrollmentStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "enrollments")
@Getter
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

    public EnrollmentEntity(UUID id, StudentEntity student, CourseEntity course, EnrollmentStatus status, Instant enrolledAt, Instant droppedAt, String finalGrade, Instant createdAt, Instant updatedAt, long version) {
        this.id = id;
        this.student = student;
        this.course = course;
        this.status = status;
        this.enrolledAt = enrolledAt;
        this.droppedAt = droppedAt;
        this.finalGrade = finalGrade;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public UUID getStudentId() {
        return student.getId();
    }

    public UUID getCourseId() {
        return course.getId();
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setStudent(StudentEntity student) {
        this.student = student;
    }

    public void setCourse(CourseEntity course) {
        this.course = course;
    }

    public void setStatus(EnrollmentStatus status) {
        this.status = status;
    }

    public void setEnrolledAt(Instant enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public void setDroppedAt(Instant droppedAt) {
        this.droppedAt = droppedAt;
    }

    public void setFinalGrade(String finalGrade) {
        this.finalGrade = finalGrade;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

}
