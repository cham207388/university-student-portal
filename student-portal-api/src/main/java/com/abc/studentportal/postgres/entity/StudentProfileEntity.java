package com.abc.studentportal.postgres.entity;

import jakarta.persistence.*;

import java.util.UUID;
import java.time.LocalDate;

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

    @Version
    @Column(nullable = false)
    private long version;

    public StudentProfileEntity(UUID id, UUID studentId, LocalDate dateOfBirth, String phone, String address) {
        this.id = studentId;
        this.dateOfBirth = dateOfBirth;
        this.phoneNumber = phone;
        this.addressLine1 = address;
    }

    public void attachToStudent(StudentEntity student) {
        this.student = student;
        this.id = student.getId();
    }

    public void updateDetails(LocalDate dateOfBirth, String phoneNumber, String addressLine1) {
        this.dateOfBirth = dateOfBirth; this.phoneNumber = phoneNumber; this.addressLine1 = addressLine1;
    }

    public UUID getId() {
        return id;
    }

    public String getAddressLine2() {
        return null;
    }

    public String getCity() {
        return "unknown";
    }

    public String getState() {
        return "unknown";
    }

    public String getPostalCode() {
        return "unknown";
    }

    public String getCountry() {
        return "unknown";
    }

}
