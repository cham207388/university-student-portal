package com.abc.studentportal.postgres.entity;

import jakarta.persistence.*;

import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Profile is a shared-primary-key extension of students (student_id is both PK and FK).
 */
@Entity
@Table(name = "student_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentProfileEntity {
    @Id
    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEntity student;
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;
    @Column(name = "phone")
    private String phoneNumber;
    @Column(name = "address")
    private String addressLine1;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    @Column(nullable = false)
    private long version;

    public StudentProfileEntity(UUID id, UUID studentId, LocalDate dateOfBirth, String phone, String address,
                                Instant createdAt, Instant updatedAt) {
        this.studentId = studentId;
        this.dateOfBirth = dateOfBirth;
        this.phoneNumber = phone;
        this.addressLine1 = address;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return studentId;
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
