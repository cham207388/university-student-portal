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

    public StudentPostgresRepository(StudentJpaRepository studentJpaRepository) {
        delegate = studentJpaRepository;
    }

    public Student create(Student student) {
        return toDomain(delegate.save(toEntity(student)));
    }

    public Student update(Student student) {
        return toDomain(delegate.save(toEntity(student)));
    }

    public Optional<Student> findById(UUID id) {
        return delegate.findById(id).map(this::toDomain);
    }

    public Optional<Student> findByStudentNumber(String studentNumber) {
        return delegate.findByStudentNumber(studentNumber).map(this::toDomain);
    }

    public Optional<Student> findByEmail(String email) {
        return delegate.findByEmail(email).map(this::toDomain);
    }

    public boolean existsByStudentNumber(String studentNumber) {
        return delegate.findByStudentNumber(studentNumber).isPresent();
    }

    public boolean existsByEmail(String email) {
        return delegate.findByEmail(email).isPresent();
    }

    public void delete(Student student) {
        delegate.deleteById(student.id());
    }

    private StudentEntity toEntity(Student student) {
        return new StudentEntity(student.id(), student.studentNumber(), student.firstName(), student.lastName(), student.email(), student.status(), student.departmentId());
    }

    private Student toDomain(StudentEntity studentEntity) {
        return new Student(studentEntity.getId(), studentEntity.getStudentNumber(), studentEntity.getFirstName(), studentEntity.getLastName(), studentEntity.getEmail(), studentEntity.getStatus(), studentEntity.getDepartment().getId(), studentEntity.getCreatedAt(), studentEntity.getUpdatedAt(), studentEntity.getVersion());
    }

}
