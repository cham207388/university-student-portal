package com.abc.studentportal.student.application;

import com.abc.studentportal.student.domain.Student;

import java.util.Optional;
import java.util.UUID;

public interface StudentRepository {

	Student save(Student student);

	Optional<Student> findById(UUID id);

	boolean existsByStudentNumber(String studentNumber);

	boolean existsByEmail(String email);

	void delete(Student student);
}
