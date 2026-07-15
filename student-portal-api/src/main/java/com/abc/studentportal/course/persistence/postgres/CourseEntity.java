package com.abc.studentportal.course.persistence.postgres;

import com.abc.studentportal.common.persistence.postgres.BaseEntity;
import com.abc.studentportal.department.persistence.postgres.DepartmentEntity;
import com.abc.studentportal.instructor.persistence.postgres.InstructorEntity;

import com.abc.studentportal.course.domain.CourseStatus;
import jakarta.persistence.*;

import java.util.UUID;
import java.time.Instant;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.abc.studentportal.common.exception.ConflictException;

@Entity
@Getter
@Table(name = "courses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseEntity extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true)
    private String courseCode;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private int credits;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "occupied_seats", nullable = false)
    private int occupiedSeats;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private DepartmentEntity department;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instructor_id", nullable = false)
    private InstructorEntity instructor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus status;

    @Version
    private long version;

    public CourseEntity(UUID id, String courseCode, String title, String description, int credits, int capacity, CourseStatus status, UUID departmentId, UUID instructorId) {
        this(id, courseCode, title, description, credits, capacity, status, departmentId, instructorId, null, null, 0);
    }

    public CourseEntity(UUID id, String courseCode, String title, String description, int credits, int capacity,
            CourseStatus status, UUID departmentId, UUID instructorId, Instant createdAt, Instant updatedAt, long version) {
        this.id = id;
        this.courseCode = courseCode;
        this.title = title;
        this.description = description;
        this.credits = credits;
        this.capacity = capacity;
        this.status = status;
        this.department = new DepartmentEntity(departmentId, null, null, null);
        this.instructor = new InstructorEntity(instructorId, null, null, null, null, departmentId);
        this.version = version;
        audit(createdAt, updatedAt);
    }

    public void updateDetails(String courseCode, String title, String description, int credits, int capacity, CourseStatus status,
            DepartmentEntity department, InstructorEntity instructor) {
        this.courseCode = courseCode; this.title = title; this.description = description;
        this.credits = credits; this.capacity = capacity; this.status = status;
        this.department = department; this.instructor = instructor;
    }

    public void reserveSeat() {
        if (occupiedSeats >= capacity) throw new ConflictException("Course capacity has been reached");
        occupiedSeats++;
    }

    public void releaseSeat() {
        if (occupiedSeats <= 0) throw new IllegalStateException("Course occupied seat count cannot become negative");
        occupiedSeats--;
    }

}
