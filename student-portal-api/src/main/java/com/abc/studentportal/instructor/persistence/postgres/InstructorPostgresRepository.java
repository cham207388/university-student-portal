package com.abc.studentportal.instructor.persistence.postgres;

import com.abc.studentportal.instructor.application.InstructorRepository;
import com.abc.studentportal.instructor.domain.Instructor;
import com.abc.studentportal.common.persistence.postgres.PostgresVersions;
import com.abc.studentportal.department.persistence.postgres.DepartmentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.*;

@Primary
@Repository
@RequiredArgsConstructor
@Profile({"local-postgres", "test-postgres", "migration"})
public class InstructorPostgresRepository implements InstructorRepository {

    private final InstructorJpaRepository instructorJpaRepository;
    private final DepartmentJpaRepository departmentJpaRepository;

    public Instructor create(Instructor instructor) {
        return toDomain(instructorJpaRepository.save(toEntity(instructor)));
    }

    public Instructor update(Instructor instructor) {
        InstructorEntity existing = instructorJpaRepository.findById(instructor.id()).orElseThrow();
        PostgresVersions.require(InstructorEntity.class, instructor.id(), instructor.version(), existing.getVersion());
        existing.updateDetails(instructor.employeeNumber(), instructor.firstName(), instructor.lastName(), instructor.email(),
                departmentJpaRepository.getReferenceById(instructor.departmentId()));
        existing.touch(instructor.updatedAt());
        return toDomain(instructorJpaRepository.save(existing));
    }

    public Optional<Instructor> findById(UUID id) {
        return instructorJpaRepository.findById(id).map(this::toDomain);
    }

    public Optional<Instructor> findByEmployeeNumber(String employeeNumber) {
        return instructorJpaRepository.findByEmployeeNumber(employeeNumber).map(this::toDomain);
    }

    public Optional<Instructor> findByEmail(String email) {
        return instructorJpaRepository.findByEmail(email).map(this::toDomain);
    }

    public boolean existsByEmployeeNumber(String employeeNumber) {
        return instructorJpaRepository.findByEmployeeNumber(employeeNumber).isPresent();
    }

    public boolean existsByEmail(String email) {
        return instructorJpaRepository.findByEmail(email).isPresent();
    }

    public void delete(Instructor instructor) {
        InstructorEntity existing = instructorJpaRepository.findById(instructor.id()).orElseThrow();
        PostgresVersions.require(InstructorEntity.class, instructor.id(), instructor.version(), existing.getVersion());
        instructorJpaRepository.delete(existing);
        instructorJpaRepository.flush();
    }

    private InstructorEntity toEntity(Instructor instructor) {
        return new InstructorEntity(instructor.id(), instructor.employeeNumber(), instructor.firstName(), instructor.lastName(),
                instructor.email(), instructor.departmentId(), instructor.createdAt(), instructor.updatedAt(), instructor.version());
    }

    private Instructor toDomain(InstructorEntity instructorEntity) {
        return new Instructor(instructorEntity.getId(), instructorEntity.getEmployeeNumber(), instructorEntity.getFirstName(), instructorEntity.getLastName(), instructorEntity.getEmail(), instructorEntity.getDepartment().getId(), instructorEntity.getCreatedAt(), instructorEntity.getUpdatedAt(), instructorEntity.getVersion());
    }

}
