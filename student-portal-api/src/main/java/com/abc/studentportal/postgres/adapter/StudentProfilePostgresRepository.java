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

    private final StudentProfileJpaRepository studentProfileJpaRepository;

    public StudentProfilePostgresRepository(StudentProfileJpaRepository studentProfileJpaRepository) {
        this.studentProfileJpaRepository = studentProfileJpaRepository;
    }

    public StudentProfile create(StudentProfile x) {
        return toDomain(studentProfileJpaRepository.save(toEntity(x)));
    }

    public StudentProfile update(StudentProfile x) {
        return toDomain(studentProfileJpaRepository.save(toEntity(x)));
    }

    public Optional<StudentProfile> findByStudentId(UUID id) {
        return studentProfileJpaRepository.findByStudentId(id).map(this::toDomain);
    }

    public void delete(StudentProfile x) {
        studentProfileJpaRepository.deleteById(x.id());
    }

    private StudentProfileEntity toEntity(StudentProfile studentProfile) {
        return new StudentProfileEntity(studentProfile.id(),
                studentProfile.studentId(), studentProfile.dateOfBirth(),
                studentProfile.phoneNumber(), studentProfile.addressLine1(),
                studentProfile.createdAt(), studentProfile.updatedAt());
    }

    private StudentProfile toDomain(StudentProfileEntity studentProfileEntity) {
        return new StudentProfile(studentProfileEntity.getId(), studentProfileEntity.getStudent().getId(),
                studentProfileEntity.getDateOfBirth(), studentProfileEntity.getPhoneNumber(),
                studentProfileEntity.getAddressLine1(), studentProfileEntity.getAddressLine2(),
                studentProfileEntity.getCity(), studentProfileEntity.getState(),
                studentProfileEntity.getPostalCode(), studentProfileEntity.getCountry(),
                studentProfileEntity.getCreatedAt(), studentProfileEntity.getUpdatedAt(),
                studentProfileEntity.getVersion());
    }

}
