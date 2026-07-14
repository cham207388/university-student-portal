package com.abc.studentportal.student.application;

import com.abc.studentportal.common.exception.ResourceNotFoundException;
import com.abc.studentportal.common.exception.ConflictException;
import com.abc.studentportal.common.application.DependencyChecker;
import com.abc.studentportal.department.application.DepartmentRepository;
import com.abc.studentportal.student.domain.Student;
import com.abc.studentportal.student.domain.StudentProfile;
import com.abc.studentportal.student.domain.StudentStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Profile({"local-dynamodb", "test-dynamodb"})
public class StudentService {
	private final StudentRepository students;
	private final StudentProfileRepository profiles;
	private final DepartmentRepository departments;
	private final Clock clock;
	private final DependencyChecker dependencies;

	public StudentService(StudentRepository students, StudentProfileRepository profiles,
			DepartmentRepository departments, Clock clock, DependencyChecker dependencies) {
		this.students = students; this.profiles = profiles; this.departments = departments; this.clock = clock;
		this.dependencies = dependencies;
	}

	public Student create(CreateCommand command) {
		requireDepartment(command.departmentId());
		Instant now = clock.instant();
		return students.create(new Student(UUID.randomUUID(), command.studentNumber(), command.firstName(), command.lastName(),
				command.email(), command.status(), command.departmentId(), now, now, 0));
	}

	public Student update(UUID id, UpdateCommand command) {
		Student current = get(id); requireDepartment(command.departmentId());
		return students.update(new Student(id, command.studentNumber(), command.firstName(), command.lastName(), command.email(),
				current.status(), command.departmentId(), current.createdAt(), clock.instant(), command.version()));
	}

	public Student changeStatus(UUID id, StudentStatus status, long version) {
		Student current = get(id);
		Student changed = current.changeStatus(status, clock.instant());
		return students.update(new Student(changed.id(), changed.studentNumber(), changed.firstName(), changed.lastName(),
				changed.email(), changed.status(), changed.departmentId(), changed.createdAt(), changed.updatedAt(), version));
	}

	public StudentProfile putProfile(UUID studentId, ProfileCommand command) {
		get(studentId);
		Instant now = clock.instant();
		return profiles.findByStudentId(studentId)
				.map(current -> profiles.update(profile(current.id(), studentId, command, current.createdAt(), now)))
				.orElseGet(() -> profiles.create(profile(UUID.randomUUID(), studentId, command, now, now)));
	}

	public Student get(UUID id) {
		return students.findById(id).orElseThrow(() -> new ResourceNotFoundException("Student", id));
	}

	public void delete(UUID id, long version) {
		Student current = get(id);
		if (dependencies.studentHasEnrollmentHistory(id)) throw new ConflictException("Student has enrollment history");
		students.delete(new Student(current.id(), current.studentNumber(), current.firstName(), current.lastName(),
				current.email(), current.status(), current.departmentId(), current.createdAt(), current.updatedAt(), version));
	}

	public void deleteProfile(UUID studentId, long version) {
		StudentProfile profile = profiles.findByStudentId(studentId)
				.orElseThrow(() -> new ResourceNotFoundException("Student profile", studentId));
		profiles.delete(new StudentProfile(profile.id(), profile.studentId(), profile.dateOfBirth(), profile.phoneNumber(),
				profile.addressLine1(), profile.addressLine2(), profile.city(), profile.state(), profile.postalCode(),
				profile.country(), profile.createdAt(), profile.updatedAt(), version));
	}

	private StudentProfile profile(UUID id, UUID studentId, ProfileCommand command, Instant createdAt, Instant updatedAt) {
		return new StudentProfile(id, studentId, command.dateOfBirth(), command.phoneNumber(), command.addressLine1(),
				command.addressLine2(), command.city(), command.state(), command.postalCode(), command.country(), createdAt,
				updatedAt, command.version());
	}

	private void requireDepartment(UUID id) {
		if (departments.findById(id).isEmpty()) throw new ResourceNotFoundException("Department", id);
	}

	public record CreateCommand(String studentNumber, String firstName, String lastName, String email,
			StudentStatus status, UUID departmentId) { }
	public record UpdateCommand(String studentNumber, String firstName, String lastName, String email,
			UUID departmentId, long version) { }
	public record ProfileCommand(LocalDate dateOfBirth, String phoneNumber, String addressLine1, String addressLine2,
			String city, String state, String postalCode, String country, long version) { }
}
