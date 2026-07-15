package com.abc.studentportal.student.persistence.postgres;

import com.abc.studentportal.common.persistence.postgres.PostgresVersions;
import com.abc.studentportal.student.application.StudentProfileRepository;
import com.abc.studentportal.student.domain.StudentProfile;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@Primary
@Profile({"local-postgres", "test-postgres", "migration"})
public class StudentProfilePostgresRepository implements StudentProfileRepository {

    private final StudentProfileJpaRepository studentProfileJpaRepository;

    private final StudentJpaRepository studentJpaRepository;

    private final EntityManager entityManager;

    public StudentProfilePostgresRepository(StudentProfileJpaRepository studentProfileJpaRepository,
                                            StudentJpaRepository studentJpaRepository, EntityManager entityManager) {

        this.studentProfileJpaRepository = studentProfileJpaRepository;
        this.studentJpaRepository = studentJpaRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public StudentProfile create(StudentProfile studentProfile) {

        StudentProfileEntity entity = toEntity(studentProfile);
        entityManager.persist(entity);
        entityManager.flush();
        return toDomain(entity);
    }

    @Transactional
    public StudentProfile update(StudentProfile studentProfile) {

        StudentProfileEntity existing = studentProfileJpaRepository.findById(studentProfile.id()).orElseThrow();
        PostgresVersions.require(StudentProfileEntity.class, studentProfile.id(), studentProfile.version(), existing.getVersion());
        existing.updateDetails(studentProfile.dateOfBirth(), studentProfile.phoneNumber(), studentProfile.addressLine1(),
                studentProfile.addressLine2(), studentProfile.city(), studentProfile.state(), studentProfile.postalCode(),
                studentProfile.country());
        existing.touch(studentProfile.updatedAt());
        return toDomain(studentProfileJpaRepository.saveAndFlush(existing));
    }

    public Optional<StudentProfile> findByStudentId(UUID id) {

        return studentProfileJpaRepository.findByStudentId(id).map(this::toDomain);
    }

    @Transactional
    public void delete(StudentProfile studentProfile) {

        StudentProfileEntity existing = studentProfileJpaRepository.findById(studentProfile.id()).orElseThrow();
        PostgresVersions.require(StudentProfileEntity.class, studentProfile.id(), studentProfile.version(), existing.getVersion());
        studentProfileJpaRepository.delete(existing);
        studentProfileJpaRepository.flush();
    }

    private StudentProfileEntity toEntity(StudentProfile studentProfile) {

        StudentProfileEntity entity = new StudentProfileEntity(studentProfile.id(),
                studentProfile.studentId(), studentProfile.dateOfBirth(),
                studentProfile.phoneNumber(), studentProfile.addressLine1(), studentProfile.createdAt(),
                studentProfile.updatedAt(), studentProfile.version());
        entity.updateDetails(studentProfile.dateOfBirth(), studentProfile.phoneNumber(), studentProfile.addressLine1(),
                studentProfile.addressLine2(), studentProfile.city(), studentProfile.state(), studentProfile.postalCode(),
                studentProfile.country());
        entity.attachToStudent(studentJpaRepository.getReferenceById(studentProfile.studentId()));
        return entity;
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
