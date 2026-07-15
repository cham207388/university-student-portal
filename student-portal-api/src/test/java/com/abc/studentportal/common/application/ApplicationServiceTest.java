package com.abc.studentportal.common.application;

import com.abc.studentportal.common.exception.InvalidRequestException;
import com.abc.studentportal.common.exception.ResourceNotFoundException;
import com.abc.studentportal.common.pagination.CursorPage;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.course.application.CourseQueries;
import com.abc.studentportal.course.application.CourseRepository;
import com.abc.studentportal.course.application.CourseService;
import com.abc.studentportal.course.domain.Course;
import com.abc.studentportal.course.domain.CourseStatus;
import com.abc.studentportal.department.application.DepartmentQueries;
import com.abc.studentportal.department.application.DepartmentRepository;
import com.abc.studentportal.department.application.DepartmentService;
import com.abc.studentportal.department.domain.Department;
import com.abc.studentportal.enrollment.application.EnrollmentQueries;
import com.abc.studentportal.enrollment.application.EnrollmentService;
import com.abc.studentportal.instructor.application.InstructorQueries;
import com.abc.studentportal.instructor.application.InstructorRepository;
import com.abc.studentportal.instructor.application.InstructorService;
import com.abc.studentportal.instructor.domain.Instructor;
import com.abc.studentportal.student.application.StudentProfileRepository;
import com.abc.studentportal.student.application.StudentQueries;
import com.abc.studentportal.student.application.StudentRepository;
import com.abc.studentportal.student.application.StudentService;
import com.abc.studentportal.student.domain.Student;
import com.abc.studentportal.student.domain.StudentStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
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
		StudentService service = studentService(students, departments, mock(DependencyChecker.class));
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
		CourseService service = courseService(courses, departments, instructors, mock(DependencyChecker.class));
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

	@Test
	void dependencyAwareDeletionRejectsStudentEnrollmentHistory() {
		StudentRepository students = mock(StudentRepository.class);
		DependencyChecker dependencies = mock(DependencyChecker.class);
		UUID studentId = UUID.randomUUID();
		UUID departmentId = UUID.randomUUID();
		Student student = new Student(studentId, "S-DELETE", "Ada", "Lovelace", "ada@example.com",
				StudentStatus.ACTIVE, departmentId, NOW, NOW, 3);
		when(students.findById(studentId)).thenReturn(Optional.of(student));
		when(dependencies.studentHasEnrollmentHistory(studentId)).thenReturn(true);
		StudentService service = studentService(students, mock(DepartmentRepository.class), dependencies);

		assertThatThrownBy(() -> service.delete(studentId, 3)).isInstanceOf(com.abc.studentportal.common.exception.ConflictException.class)
				.hasMessage("Student has enrollment history");
		verify(students, never()).delete(any());
	}

	@Test
	void studentListRejectsMultipleFiltersAndRequiresDepartmentForLastName() {
		StudentQueries queries = mock(StudentQueries.class);
		StudentService service = new StudentService(mock(StudentRepository.class), mock(StudentProfileRepository.class),
				mock(DepartmentRepository.class), CLOCK, mock(DependencyChecker.class), queries,
				mock(EnrollmentQueries.class), mock(StudentCourseQueries.class));

		assertThatThrownBy(() -> service.list(new StudentService.StudentListQuery(UUID.randomUUID(),
				StudentStatus.ACTIVE, null, null, null, 20, null)))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessage("Student lists support one filter at a time");
		assertThatThrownBy(() -> service.list(new StudentService.StudentListQuery(null, null, "Morgan", null, null, 20,
				null)))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessage("lastName requires departmentId");
	}

	@Test
	void studentListPassesLastNameWithDepartmentToQueries() {
		StudentQueries queries = mock(StudentQueries.class);
		UUID departmentId = UUID.randomUUID();
		CursorPage<Student> expected = new CursorPage<>(List.of(), 20, null, false);
		when(queries.findByDepartment(departmentId, "Morgan", new CursorRequest(20, null))).thenReturn(expected);
		StudentService service = new StudentService(mock(StudentRepository.class), mock(StudentProfileRepository.class),
				mock(DepartmentRepository.class), CLOCK, mock(DependencyChecker.class), queries,
				mock(EnrollmentQueries.class), mock(StudentCourseQueries.class));

		assertThat(service.list(new StudentService.StudentListQuery(departmentId, null, "Morgan", null, null, 20, null)))
				.isSameAs(expected);
		verify(queries).findByDepartment(departmentId, "Morgan", new CursorRequest(20, null));
	}

	@Test
	void courseAndInstructorListsRejectCombinedFiltersAndExactLookupWithCursor() {
		CourseQueries courseQueries = mock(CourseQueries.class);
		CourseService courses = new CourseService(mock(CourseRepository.class), mock(DepartmentRepository.class),
				mock(InstructorRepository.class), CLOCK, mock(DependencyChecker.class), courseQueries,
				mock(EnrollmentQueries.class), mock(StudentCourseQueries.class));
		assertThatThrownBy(() -> courses.list(new CourseService.CourseListQuery(UUID.randomUUID(), null,
				CourseStatus.OPEN, null, 20, null)))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessage("Course lists support one filter at a time");
		assertThatThrownBy(() -> courses.list(new CourseService.CourseListQuery(null, null, null, "CS101", 20, "c")))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessage("cursor cannot be combined with an exact courseCode lookup");

		InstructorService instructors = new InstructorService(mock(InstructorRepository.class),
				mock(DepartmentRepository.class), CLOCK, mock(DependencyChecker.class), mock(InstructorQueries.class),
				mock(CourseQueries.class));
		assertThatThrownBy(() -> instructors.list(new InstructorService.InstructorListQuery(UUID.randomUUID(), "E1",
				null, 20, null)))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessage("Instructor lists support one filter at a time");
		assertThatThrownBy(() -> instructors.list(new InstructorService.InstructorListQuery(null, null, "a@b.com", 20,
				"c")))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessage("cursor cannot be combined with an exact instructor lookup");
	}

	@Test
	void studentExactLookupRejectsCursorAndReturnsEmptyPage() {
		StudentRepository students = mock(StudentRepository.class);
		when(students.findByStudentNumber("missing")).thenReturn(Optional.empty());
		StudentService service = studentService(students, mock(DepartmentRepository.class),
				mock(DependencyChecker.class));

		assertThatThrownBy(() -> service.list(new StudentService.StudentListQuery(null, null, null, "S1", null, 20,
				"cursor")))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessage("cursor cannot be combined with an exact student lookup");

		CursorPage<Student> page = service.list(new StudentService.StudentListQuery(null, null, null, "missing", null,
				20, null));
		assertThat(page.content()).isEmpty();
		assertThat(page.limit()).isEqualTo(1);
		assertThat(page.hasNext()).isFalse();
	}

	@Test
	void nestedStudentCoursesRequireExistingStudent() {
		StudentRepository students = mock(StudentRepository.class);
		UUID studentId = UUID.randomUUID();
		when(students.findById(studentId)).thenReturn(Optional.empty());
		StudentService service = studentService(students, mock(DepartmentRepository.class),
				mock(DependencyChecker.class));

		assertThatThrownBy(() -> service.listCourses(studentId, new CursorRequest(20, null)))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void enrollmentListRejectsCombinedFiltersAndOrphanDateRanges() {
		EnrollmentService service = new EnrollmentService(mock(com.abc.studentportal.enrollment.application.EnrollmentRepository.class),
				mock(StudentRepository.class), mock(CourseRepository.class), CLOCK, mock(EnrollmentQueries.class));

		assertThatThrownBy(() -> service.list(new EnrollmentService.EnrollmentListQuery(UUID.randomUUID(),
				UUID.randomUUID(), null, null, null, 20, null)))
				.isInstanceOf(InvalidRequestException.class);
		assertThatThrownBy(() -> service.list(new EnrollmentService.EnrollmentListQuery(null, null, null, NOW, null, 20,
				null)))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessage("Enrollment date ranges require studentId or courseId");
	}

	@Test
	void departmentExactCodeRejectsCursor() {
		DepartmentService service = new DepartmentService(mock(DepartmentRepository.class), CLOCK,
				mock(DependencyChecker.class), mock(DepartmentQueries.class), mock(StudentQueries.class),
				mock(InstructorQueries.class), mock(CourseQueries.class));

		assertThatThrownBy(() -> service.list(new DepartmentService.DepartmentListQuery("CS", 20, "opaque")))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessage("cursor cannot be combined with exact code lookup");
	}

	private static StudentService studentService(StudentRepository students, DepartmentRepository departments,
			DependencyChecker dependencies) {
		return new StudentService(students, mock(StudentProfileRepository.class), departments, CLOCK, dependencies,
				mock(StudentQueries.class), mock(EnrollmentQueries.class), mock(StudentCourseQueries.class));
	}

	private static CourseService courseService(CourseRepository courses, DepartmentRepository departments,
			InstructorRepository instructors, DependencyChecker dependencies) {
		return new CourseService(courses, departments, instructors, CLOCK, dependencies, mock(CourseQueries.class),
				mock(EnrollmentQueries.class), mock(StudentCourseQueries.class));
	}

	private static Department department(UUID id) {
		return new Department(id, "CS", "Computing", null, NOW, NOW, 1);
	}
}
