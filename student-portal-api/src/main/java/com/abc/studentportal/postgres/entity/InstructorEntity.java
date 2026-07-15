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
public class InstructorEntity {
    @Id
    private UUID id;
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
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    public void setDepartment(DepartmentEntity d) {
        department = d;
    }

    public InstructorEntity(UUID id, String en, String fn, String ln, String email, UUID dept, Instant c, Instant u) {
        this.id = id;
        employeeNumber = en;
        firstName = fn;
        lastName = ln;
        this.email = email;
        department = new DepartmentEntity(dept, null, null, null, c, u);
        createdAt = c;
        updatedAt = u;
    }

}
