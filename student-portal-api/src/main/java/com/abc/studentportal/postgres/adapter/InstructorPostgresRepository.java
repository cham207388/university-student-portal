package com.abc.studentportal.postgres.adapter;

import com.abc.studentportal.instructor.application.InstructorRepository;
import com.abc.studentportal.instructor.domain.Instructor;
import com.abc.studentportal.postgres.entity.InstructorEntity;
import com.abc.studentportal.postgres.repository.InstructorJpaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@Profile({"local-postgres", "test-postgres"})
public class InstructorPostgresRepository implements InstructorRepository {

    private final InstructorJpaRepository instructorJpaRepository;

    public InstructorPostgresRepository(InstructorJpaRepository instructorJpaRepository) {
        this.instructorJpaRepository = instructorJpaRepository;
    }

    public Instructor create(Instructor instructor) {
        return toDomain(instructorJpaRepository.save(toEntity(instructor)));
    }

    public Instructor update(Instructor instructor) {
        return toDomain(instructorJpaRepository.save(toEntity(instructor)));
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
        instructorJpaRepository.deleteById(instructor.id());
    }

    private InstructorEntity toEntity(Instructor instructor) {
        return new InstructorEntity(instructor.id(), instructor.employeeNumber(), instructor.firstName(), instructor.lastName(), instructor.email(), instructor.departmentId());
    }

    private Instructor toDomain(InstructorEntity instructorEntity) {
        return new Instructor(instructorEntity.getId(), instructorEntity.getEmployeeNumber(), instructorEntity.getFirstName(), instructorEntity.getLastName(), instructorEntity.getEmail(), instructorEntity.getDepartment().getId(), instructorEntity.getCreatedAt(), instructorEntity.getUpdatedAt(), instructorEntity.getVersion());
    }

}
