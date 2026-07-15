package com.abc.studentportal.student.persistence.postgres;

import com.abc.studentportal.common.persistence.postgres.BaseEntity;

import jakarta.persistence.*;

import java.util.UUID;
import java.time.LocalDate;
import java.time.Instant;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Profile is a shared-primary-key extension of students (student_id is both PK and FK).
 */
@Entity
@Getter
@Table(name = "student_profiles")
@AttributeOverride(name = "id", column = @Column(name = "student_id", nullable = false))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentProfileEntity extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @PrimaryKeyJoinColumn(name = "student_id", referencedColumnName = "id")
    private StudentEntity student;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "phone")
    private String phoneNumber;

    @Column(name = "address")
    private String addressLine1;

    @Column(name = "address_line_2")
    private String addressLine2;

    private String city;
    private String state;

    @Column(name = "postal_code")
    private String postalCode;

    private String country;

    @Version
    @Column(nullable = false)
    private long version;

    public StudentProfileEntity(UUID id, UUID studentId, LocalDate dateOfBirth, String phone, String address) {
        this(id, studentId, dateOfBirth, phone, address, null, null, 0);
    }

    public StudentProfileEntity(UUID id, UUID studentId, LocalDate dateOfBirth, String phone, String address,
            Instant createdAt, Instant updatedAt, long version) {
        this(id, studentId, dateOfBirth, phone, address, null, "unknown", "unknown", "unknown", "unknown",
                createdAt, updatedAt, version);
    }

    public StudentProfileEntity(UUID id, UUID studentId, LocalDate dateOfBirth, String phone, String addressLine1,
            String addressLine2, String city, String state, String postalCode, String country,
            Instant createdAt, Instant updatedAt, long version) {
        this.id = studentId;
        this.dateOfBirth = dateOfBirth;
        this.phoneNumber = phone;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
        this.version = version;
        audit(createdAt, updatedAt);
    }

    public void attachToStudent(StudentEntity student) {
        this.student = student;
        this.id = student.getId();
    }

    public void updateDetails(LocalDate dateOfBirth, String phoneNumber, String addressLine1) {
        updateDetails(dateOfBirth, phoneNumber, addressLine1, addressLine2, city, state, postalCode, country);
    }

    public void updateDetails(LocalDate dateOfBirth, String phoneNumber, String addressLine1, String addressLine2,
            String city, String state, String postalCode, String country) {
        this.dateOfBirth = dateOfBirth; this.phoneNumber = phoneNumber; this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2; this.city = city; this.state = state; this.postalCode = postalCode;
        this.country = country;
    }

    public UUID getId() {
        return id;
    }

}
