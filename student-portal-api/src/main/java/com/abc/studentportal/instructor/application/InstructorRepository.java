package com.abc.studentportal.instructor.application;

import com.abc.studentportal.instructor.domain.Instructor;

import java.util.Optional;
import java.util.UUID;

public interface InstructorRepository {

	Instructor create(Instructor instructor);

	Instructor update(Instructor instructor);

	Optional<Instructor> findById(UUID id);

	boolean existsByEmployeeNumber(String employeeNumber);

	boolean existsByEmail(String email);

	void delete(Instructor instructor);
}
