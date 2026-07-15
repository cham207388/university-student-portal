package com.abc.studentportal.instructor.persistence.postgres;

import com.abc.studentportal.common.persistence.postgres.BaseEntity;
import com.abc.studentportal.department.persistence.postgres.DepartmentEntity;

import jakarta.persistence.*;

import java.util.UUID;
import java.time.Instant;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Table(name = "instructors")
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

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private DepartmentEntity department;

    @Version
    private long version;

    public InstructorEntity(UUID id, String employeeNumber, String firstName, String lastName, String email, UUID departmentId) {
        this(id, employeeNumber, firstName, lastName, email, departmentId, null, null, 0);
    }

    public InstructorEntity(UUID id, String employeeNumber, String firstName, String lastName, String email, UUID departmentId,
            Instant createdAt, Instant updatedAt, long version) {
        this.id = id;
        this.employeeNumber = employeeNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        department = new DepartmentEntity(departmentId, null, null, null);
        this.version = version;
        audit(createdAt, updatedAt);
    }

    public void updateDetails(String employeeNumber, String firstName, String lastName, String email, DepartmentEntity department) {
        this.employeeNumber = employeeNumber; this.firstName = firstName; this.lastName = lastName;
        this.email = email; this.department = department;
    }

}
