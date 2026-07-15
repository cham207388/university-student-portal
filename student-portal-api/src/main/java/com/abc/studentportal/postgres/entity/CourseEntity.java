package com.abc.studentportal.postgres.entity;

import com.abc.studentportal.course.domain.CourseStatus;
import jakarta.persistence.*;

import java.util.UUID;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
        this.id = id;
        this.courseCode = courseCode;
        this.title = title;
        this.description = description;
        this.credits = credits;
        this.capacity = capacity;
        this.status = status;
        this.department = new DepartmentEntity(departmentId, null, null, null);
        this.instructor = new InstructorEntity(instructorId, null, null, null, null, departmentId);
    }

}
