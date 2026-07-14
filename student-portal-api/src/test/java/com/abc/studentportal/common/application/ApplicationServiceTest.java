package com.abc.studentportal.common.application;

import com.abc.studentportal.common.exception.InvalidRequestException;
import com.abc.studentportal.common.exception.ResourceNotFoundException;
import com.abc.studentportal.course.application.CourseRepository;
import com.abc.studentportal.course.application.CourseService;
import com.abc.studentportal.course.domain.Course;
import com.abc.studentportal.course.domain.CourseStatus;
import com.abc.studentportal.department.application.DepartmentRepository;
import com.abc.studentportal.department.domain.Department;
import com.abc.studentportal.instructor.application.InstructorRepository;
import com.abc.studentportal.instructor.domain.Instructor;
import com.abc.studentportal.student.application.StudentProfileRepository;
import com.abc.studentportal.student.application.StudentRepository;
import com.abc.studentportal.student.application.StudentService;
import com.abc.studentportal.student.domain.Student;
import com.abc.studentportal.student.domain.StudentStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplicationServiceTest {
	private static final Instant NOW = Instant.parse("2026-05-01T12:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	@Test
	void studentCreationRequiresAnExistingDepartment() {
		StudentRepository students = mock(StudentRepository.class);
		DepartmentRepository departments = mock(DepartmentRepository.class);
		StudentService service = new StudentService(students, mock(StudentProfileRepository.class), departments, CLOCK);
		UUID departmentId = UUID.randomUUID();
		var command = new StudentService.CreateCommand("S-1", "Ada", "Lovelace", "ADA@EXAMPLE.COM",
				StudentStatus.ACTIVE, departmentId);

		assertThatThrownBy(() -> service.create(command)).isInstanceOf(ResourceNotFoundException.class);
		verify(students, never()).create(any());

		when(departments.findById(departmentId)).thenReturn(Optional.of(department(departmentId)));
		when(students.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
		Student created = service.create(command);
		assertThat(created.departmentId()).isEqualTo(departmentId);
		assertThat(created.email()).isEqualTo("ada@example.com");
		assertThat(created.createdAt()).isEqualTo(NOW);
	}

	@Test
	void courseCreationRequiresInstructorAndCourseToShareADepartment() {
		CourseRepository courses = mock(CourseRepository.class);
		DepartmentRepository departments = mock(DepartmentRepository.class);
		InstructorRepository instructors = mock(InstructorRepository.class);
		CourseService service = new CourseService(courses, departments, instructors, CLOCK);
		UUID departmentId = UUID.randomUUID();
		UUID instructorId = UUID.randomUUID();
		when(departments.findById(departmentId)).thenReturn(Optional.of(department(departmentId)));
		when(instructors.findById(instructorId)).thenReturn(Optional.of(new Instructor(instructorId, "E-1", "Grace",
				"Hopper", "grace@example.com", UUID.randomUUID(), NOW, NOW, 1)));
		var command = new CourseService.CreateCommand("CS-1", "Computing", null, 3, 20, CourseStatus.OPEN,
				departmentId, instructorId);

		assertThatThrownBy(() -> service.create(command)).isInstanceOf(InvalidRequestException.class)
				.hasMessage("Instructor must belong to the course department");
		verify(courses, never()).create(any());

		when(instructors.findById(instructorId)).thenReturn(Optional.of(new Instructor(instructorId, "E-1", "Grace",
				"Hopper", "grace@example.com", departmentId, NOW, NOW, 1)));
		when(courses.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
		Course created = service.create(command);
		assertThat(created.departmentId()).isEqualTo(departmentId);
		assertThat(created.instructorId()).isEqualTo(instructorId);
	}

	private static Department department(UUID id) {
		return new Department(id, "CS", "Computing", null, NOW, NOW, 1);
	}
}
