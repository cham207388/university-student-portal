package com.abc.studentportal.student.persistence.postgres;

import com.abc.studentportal.common.persistence.postgres.BaseEntity;
import com.abc.studentportal.department.persistence.postgres.DepartmentEntity;
import com.abc.studentportal.student.domain.StudentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Table(name = "students")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentEntity extends BaseEntity {

    @Column(name = "student_number", nullable = false, unique = true)
    private String studentNumber;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudentStatus status;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private DepartmentEntity department;

    @Version
    private long version;

    @OneToOne(mappedBy = "student", fetch = FetchType.LAZY, orphanRemoval = true)
    private StudentProfileEntity profile;

    public StudentEntity(UUID id, String studentNumber, String firstName, String lastName, String email, StudentStatus status, UUID departmentId) {

        this(id, studentNumber, firstName, lastName, email, status, departmentId, null, null, 0);
    }

    public StudentEntity(UUID id, String studentNumber, String firstName, String lastName, String email, StudentStatus status,
                         UUID departmentId, Instant createdAt, Instant updatedAt, long version) {

        this.id = id;
        this.studentNumber = studentNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.status = status;
        this.department = new DepartmentEntity(departmentId, null, null, null);
        this.version = version;
        audit(createdAt, updatedAt);
    }

    public void updateDetails(String studentNumber, String firstName, String lastName, String email, StudentStatus status, DepartmentEntity department) {

        this.studentNumber = studentNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.status = status;
        this.department = department;
    }

}
