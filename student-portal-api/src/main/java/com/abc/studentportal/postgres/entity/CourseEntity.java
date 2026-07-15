package com.abc.studentportal.postgres.entity;

import com.abc.studentportal.course.domain.CourseStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "courses")
@Getter
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private DepartmentEntity department;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instructor_id", nullable = false)
    private InstructorEntity instructor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus status;

    @Version
    private long version;

    public void setDepartment(DepartmentEntity department) {
        this.department = department;
    }

    public void setInstructor(InstructorEntity instructor) {
        this.instructor = instructor;
    }

    public CourseEntity(UUID id, String courseCode, String title, String description, int credits, int capacity, CourseStatus status, UUID departmentId, UUID instructorId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.courseCode = courseCode;
        this.title = title;
        this.description = description;
        this.credits = credits;
        this.capacity = capacity;
        this.status = status;
        this.department = new DepartmentEntity(departmentId, null, null, null, createdAt, updatedAt);
        this.instructor = new InstructorEntity(instructorId, null, null, null, null, departmentId, createdAt, updatedAt);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

}
