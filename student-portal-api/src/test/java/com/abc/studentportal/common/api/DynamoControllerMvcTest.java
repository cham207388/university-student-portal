package com.abc.studentportal.common.api;

import com.abc.studentportal.common.exception.GlobalExceptionHandler;
import com.abc.studentportal.common.application.DynamoStudentCourseQueries;
import com.abc.studentportal.common.pagination.CursorPage;
import com.abc.studentportal.course.api.CourseController;
import com.abc.studentportal.course.application.CourseService;
import com.abc.studentportal.course.application.DynamoCourseQueries;
import com.abc.studentportal.course.domain.Course;
import com.abc.studentportal.course.domain.CourseStatus;
import com.abc.studentportal.department.api.DepartmentController;
import com.abc.studentportal.department.application.DepartmentService;
import com.abc.studentportal.department.application.DynamoDepartmentQueries;
import com.abc.studentportal.department.domain.Department;
import com.abc.studentportal.enrollment.api.EnrollmentController;
import com.abc.studentportal.enrollment.application.DynamoEnrollmentQueries;
import com.abc.studentportal.enrollment.application.EnrollmentService;
import com.abc.studentportal.instructor.api.InstructorController;
import com.abc.studentportal.instructor.application.DynamoInstructorQueries;
import com.abc.studentportal.instructor.application.InstructorService;
import com.abc.studentportal.instructor.domain.Instructor;
import com.abc.studentportal.student.api.StudentController;
import com.abc.studentportal.student.application.DynamoStudentQueries;
import com.abc.studentportal.student.application.StudentService;
import com.abc.studentportal.student.domain.Student;
import com.abc.studentportal.student.domain.StudentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({DepartmentController.class, StudentController.class, InstructorController.class,
		CourseController.class, EnrollmentController.class})
@ActiveProfiles("test-dynamodb")
@Import(GlobalExceptionHandler.class)
class DynamoControllerMvcTest {
	@Autowired MockMvc mvc;
	@MockitoBean DepartmentService departments;
	@MockitoBean StudentService students;
	@MockitoBean InstructorService instructors;
	@MockitoBean CourseService courses;
	@MockitoBean EnrollmentService enrollments;
	@MockitoBean DynamoDepartmentQueries departmentQueries;
	@MockitoBean DynamoStudentQueries studentQueries;
	@MockitoBean DynamoInstructorQueries instructorQueries;
	@MockitoBean DynamoCourseQueries courseQueries;
	@MockitoBean DynamoEnrollmentQueries enrollmentQueries;
	@MockitoBean DynamoStudentCourseQueries relationshipQueries;

	@Test
	void createsDepartmentWithLocationAndResponseDto() throws Exception {
		UUID id = UUID.randomUUID(); Instant now = Instant.parse("2026-07-14T00:00:00Z");
		when(departments.create(any())).thenReturn(new Department(id, "CS", "Computing", null, now, now, 1));
		mvc.perform(post("/api/v1/departments").contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"CS\",\"name\":\"Computing\",\"description\":null}"))
				.andExpect(status().isCreated()).andExpect(header().string("Location", "/api/v1/departments/" + id))
				.andExpect(jsonPath("$.id").value(id.toString())).andExpect(jsonPath("$.version").value(1));
	}

	@Test
	void returnsOpaqueCursorPage() throws Exception {
		Instant now = Instant.parse("2026-07-14T00:00:00Z");
		when(departmentQueries.findAll(any())).thenReturn(new CursorPage<>(List.of(
				new Department(UUID.randomUUID(), "CS", "Computing", null, now, now, 1)), 1, "opaque", true));
		mvc.perform(get("/api/v1/departments?limit=1"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.nextCursor").value("opaque")).andExpect(jsonPath("$.hasNext").value(true));
	}

	@Test
	void validatesBodiesAndDynamoFilterCombinations() throws Exception {
		mvc.perform(post("/api/v1/students").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.type")
						.value("https://student-portal.example/problems/validation-failed"));
		mvc.perform(get("/api/v1/courses?departmentId=" + UUID.randomUUID() + "&status=OPEN"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.type")
						.value("https://student-portal.example/problems/invalid-request"));
	}

	@Test
	void exposesNormalizedExactAlternateKeyFilters() throws Exception {
		Instant now = Instant.parse("2026-07-14T00:00:00Z");
		UUID departmentId = UUID.randomUUID(); UUID instructorId = UUID.randomUUID();
		Department department = new Department(departmentId, "CS", "Computing", null, now, now, 1);
		Student student = new Student(UUID.randomUUID(), "S1", "Avery", "Morgan", "avery@example.com",
				StudentStatus.ACTIVE, departmentId, now, now, 1);
		Instructor instructor = new Instructor(instructorId, "E1", "Mira", "Chen", "mira@example.com",
				departmentId, now, now, 1);
		Course course = new Course(UUID.randomUUID(), "CS101", "Computing", null, 3, 20, CourseStatus.OPEN,
				departmentId, instructorId, now, now, 1);
		when(departments.findByCode("cs")).thenReturn(Optional.of(department));
		when(students.findByEmail(anyString())).thenReturn(Optional.of(student));
		when(instructors.findByEmployeeNumber("E1")).thenReturn(Optional.of(instructor));
		when(courses.findByCourseCode("cs101")).thenReturn(Optional.of(course));

		mvc.perform(get("/api/v1/departments?code=cs")).andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].code").value("CS")).andExpect(jsonPath("$.limit").value(1));
		mvc.perform(get("/api/v1/students?email=AVERY%40EXAMPLE.COM")).andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].studentNumber").value("S1"));
		mvc.perform(get("/api/v1/instructors?employeeNumber=E1")).andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].employeeNumber").value("E1"));
		mvc.perform(get("/api/v1/courses?courseCode=cs101")).andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].courseCode").value("CS101"));
	}

	@Test
	void returnsEmptyExactResultAndRejectsUnknownOrIncompatibleParameters() throws Exception {
		when(students.findByStudentNumber("missing")).thenReturn(Optional.empty());
		mvc.perform(get("/api/v1/students?studentNumber=missing")).andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(0));
		mvc.perform(get("/api/v1/courses?title=ignored")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Unsupported query parameter(s): title"));
		mvc.perform(get("/api/v1/students?sort=lastName&minimumCredits=2")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.type").value("https://student-portal.example/problems/invalid-request"));
		mvc.perform(get("/api/v1/students?email=a%40example.com&status=ACTIVE"))
				.andExpect(status().isBadRequest());
		mvc.perform(get("/api/v1/departments?code=CS&cursor=opaque")).andExpect(status().isBadRequest());
	}

	@Test
	void forwardsOptimisticVersionOnPhysicalDelete() throws Exception {
		UUID id = UUID.randomUUID();
		mvc.perform(delete("/api/v1/instructors/" + id + "?version=7")).andExpect(status().isNoContent());
		verify(instructors).delete(id, 7);
	}

	@Test
	void rejectsMissingVersionsAndMalformedPathIdsAsBadRequests() throws Exception {
		mvc.perform(delete("/api/v1/departments/" + UUID.randomUUID())).andExpect(status().isBadRequest());
		mvc.perform(get("/api/v1/students/not-a-uuid")).andExpect(status().isBadRequest());
	}

	@Test
	void exposesDerivedStudentCourseRelationshipPages() throws Exception {
		Instant now = Instant.parse("2026-07-14T00:00:00Z"); UUID studentId = UUID.randomUUID(); UUID courseId = UUID.randomUUID();
		UUID departmentId = UUID.randomUUID(); UUID instructorId = UUID.randomUUID();
		when(students.get(studentId)).thenReturn(new Student(studentId, "S1", "Avery", "Morgan", "avery@example.com",
				StudentStatus.ACTIVE, departmentId, now, now, 1));
		when(courses.get(courseId)).thenReturn(new Course(courseId, "CS1", "Computing", null, 3, 20, CourseStatus.OPEN,
				departmentId, instructorId, now, now, 1));
		when(relationshipQueries.findCoursesByStudent(any(), any())).thenReturn(new CursorPage<>(List.of(
				new Course(courseId, "CS1", "Computing", null, 3, 20, CourseStatus.OPEN, departmentId, instructorId, now, now, 1)), 20, null, false));
		when(relationshipQueries.findStudentsByCourse(any(), any())).thenReturn(new CursorPage<>(List.of(
				new Student(studentId, "S1", "Avery", "Morgan", "avery@example.com", StudentStatus.ACTIVE,
						departmentId, now, now, 1)), 20, null, false));

		mvc.perform(get("/api/v1/students/" + studentId + "/courses"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(courseId.toString()));
		mvc.perform(get("/api/v1/courses/" + courseId + "/students"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(studentId.toString()));
	}
}
