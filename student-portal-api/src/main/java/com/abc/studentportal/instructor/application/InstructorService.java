package com.abc.studentportal.instructor.application;

import com.abc.studentportal.common.exception.ResourceNotFoundException;
import com.abc.studentportal.common.exception.ConflictException;
import com.abc.studentportal.common.application.DependencyChecker;
import com.abc.studentportal.department.application.DepartmentRepository;
import com.abc.studentportal.instructor.domain.Instructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@Profile({"local-dynamodb", "test-dynamodb"})
public class InstructorService {
	private final InstructorRepository instructors;
	private final DepartmentRepository departments;
	private final Clock clock;
	private final DependencyChecker dependencies;

	public InstructorService(InstructorRepository instructors, DepartmentRepository departments, Clock clock,
			DependencyChecker dependencies) {
		this.instructors = instructors; this.departments = departments; this.clock = clock; this.dependencies = dependencies;
	}

	public Instructor create(CreateCommand command) {
		requireDepartment(command.departmentId());
		Instant now = clock.instant();
		return instructors.create(new Instructor(UUID.randomUUID(), command.employeeNumber(), command.firstName(),
				command.lastName(), command.email(), command.departmentId(), now, now, 0));
	}

	public Instructor update(UUID id, UpdateCommand command) {
		Instructor current = get(id); requireDepartment(command.departmentId());
		return instructors.update(new Instructor(id, command.employeeNumber(), command.firstName(), command.lastName(),
				command.email(), command.departmentId(), current.createdAt(), clock.instant(), command.version()));
	}

	public Instructor get(UUID id) {
		return instructors.findById(id).orElseThrow(() -> new ResourceNotFoundException("Instructor", id));
	}

	public void delete(UUID id, long version) {
		Instructor current = get(id);
		if (dependencies.instructorHasCourses(id)) throw new ConflictException("Instructor still has assigned courses");
		instructors.delete(new Instructor(current.id(), current.employeeNumber(), current.firstName(), current.lastName(),
				current.email(), current.departmentId(), current.createdAt(), current.updatedAt(), version));
	}

	private void requireDepartment(UUID id) {
		if (departments.findById(id).isEmpty()) throw new ResourceNotFoundException("Department", id);
	}

	public record CreateCommand(String employeeNumber, String firstName, String lastName, String email,
			UUID departmentId) { }
	public record UpdateCommand(String employeeNumber, String firstName, String lastName, String email,
			UUID departmentId, long version) { }
}
