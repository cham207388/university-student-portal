package com.abc.studentportal.postgres.adapter;

import com.abc.studentportal.student.application.StudentRepository;
import com.abc.studentportal.student.domain.Student;
import com.abc.studentportal.postgres.entity.StudentEntity;
import com.abc.studentportal.postgres.repository.StudentJpaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@Profile({"local-postgres", "test-postgres"})
public class StudentPostgresRepository implements StudentRepository {

    private final StudentJpaRepository delegate;

    public StudentPostgresRepository(StudentJpaRepository d) {
        delegate = d;
    }

    public Student create(Student s) {
        return toDomain(delegate.save(toEntity(s)));
    }

    public Student update(Student s) {
        return toDomain(delegate.save(toEntity(s)));
    }

    public Optional<Student> findById(UUID id) {
        return delegate.findById(id).map(this::toDomain);
    }

    public Optional<Student> findByStudentNumber(String n) {
        return delegate.findByStudentNumber(n).map(this::toDomain);
    }

    public Optional<Student> findByEmail(String e) {
        return delegate.findByEmail(e).map(this::toDomain);
    }

    public boolean existsByStudentNumber(String n) {
        return delegate.findByStudentNumber(n).isPresent();
    }

    public boolean existsByEmail(String e) {
        return delegate.findByEmail(e).isPresent();
    }

    public void delete(Student s) {
        delegate.deleteById(s.id());
    }

    private StudentEntity toEntity(Student s) {
        return new StudentEntity(s.id(), s.studentNumber(), s.firstName(), s.lastName(), s.email(), s.status(), s.departmentId(), s.createdAt(), s.updatedAt());
    }

    private Student toDomain(StudentEntity e) {
        return new Student(e.getId(), e.getStudentNumber(), e.getFirstName(), e.getLastName(), e.getEmail(), e.getStatus(), e.getDepartment().getId(), e.getCreatedAt(), e.getUpdatedAt(), e.getVersion());
    }

}
