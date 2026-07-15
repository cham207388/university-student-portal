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

    private final InstructorJpaRepository d;

    public InstructorPostgresRepository(InstructorJpaRepository d) {
        this.d = d;
    }

    public Instructor create(Instructor x) {
        return toDomain(d.save(toEntity(x)));
    }

    public Instructor update(Instructor x) {
        return toDomain(d.save(toEntity(x)));
    }

    public Optional<Instructor> findById(UUID id) {
        return d.findById(id).map(this::toDomain);
    }

    public Optional<Instructor> findByEmployeeNumber(String n) {
        return d.findByEmployeeNumber(n).map(this::toDomain);
    }

    public Optional<Instructor> findByEmail(String e) {
        return d.findByEmail(e).map(this::toDomain);
    }

    public boolean existsByEmployeeNumber(String n) {
        return d.findByEmployeeNumber(n).isPresent();
    }

    public boolean existsByEmail(String e) {
        return d.findByEmail(e).isPresent();
    }

    public void delete(Instructor x) {
        d.deleteById(x.id());
    }

    private InstructorEntity toEntity(Instructor x) {
        return new InstructorEntity(x.id(), x.employeeNumber(), x.firstName(), x.lastName(), x.email(), x.departmentId(), x.createdAt(), x.updatedAt());
    }

    private Instructor toDomain(InstructorEntity e) {
        return new Instructor(e.getId(), e.getEmployeeNumber(), e.getFirstName(), e.getLastName(), e.getEmail(), e.getDepartment().getId(), e.getCreatedAt(), e.getUpdatedAt(), e.getVersion());
    }

}
