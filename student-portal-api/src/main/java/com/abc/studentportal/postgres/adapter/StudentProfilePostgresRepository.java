package com.abc.studentportal.postgres.adapter;

import com.abc.studentportal.student.application.StudentProfileRepository;
import com.abc.studentportal.student.domain.StudentProfile;
import com.abc.studentportal.postgres.entity.StudentProfileEntity;
import com.abc.studentportal.postgres.repository.StudentProfileJpaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@Profile({"local-postgres", "test-postgres"})
public class StudentProfilePostgresRepository implements StudentProfileRepository {

    private final StudentProfileJpaRepository d;

    public StudentProfilePostgresRepository(StudentProfileJpaRepository d) {
        this.d = d;
    }

    public StudentProfile create(StudentProfile x) {
        return toDomain(d.save(toEntity(x)));
    }

    public StudentProfile update(StudentProfile x) {
        return toDomain(d.save(toEntity(x)));
    }

    public Optional<StudentProfile> findByStudentId(UUID id) {
        return d.findByStudentId(id).map(this::toDomain);
    }

    public void delete(StudentProfile x) {
        d.deleteById(x.id());
    }

    private StudentProfileEntity toEntity(StudentProfile x) {
        return new StudentProfileEntity(x.id(), x.studentId(), x.dateOfBirth(), x.phoneNumber(), x.addressLine1(), x.createdAt(), x.updatedAt());
    }

    private StudentProfile toDomain(StudentProfileEntity e) {
        return new StudentProfile(e.getId(), e.getStudent().getId(), e.getDateOfBirth(), e.getPhoneNumber(), e.getAddressLine1(), e.getAddressLine2(), e.getCity(), e.getState(), e.getPostalCode(), e.getCountry(), e.getCreatedAt(), e.getUpdatedAt(), e.getVersion());
    }

}
