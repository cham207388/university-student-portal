package com.abc.studentportal.postgres.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "instructors")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstructorEntity extends BaseEntity {

    @Column(name = "employee_number", nullable = false, unique = true)
    private String employeeNumber;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private DepartmentEntity department;

    @Version
    private long version;

    public void setDepartment(DepartmentEntity department) {
        this.department = department;
    }

    public InstructorEntity(UUID id, String employeeNumber, String firstName, String lastName, String email, UUID departmentId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.employeeNumber = employeeNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        department = new DepartmentEntity(departmentId, null, null, null, createdAt, updatedAt);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

}
