package com.abc.studentportal.common.configuration;

import com.abc.studentportal.course.application.CourseRepository;
import com.abc.studentportal.course.application.CourseService;
import com.abc.studentportal.department.application.DepartmentRepository;
import com.abc.studentportal.department.application.DepartmentService;
import com.abc.studentportal.enrollment.application.EnrollmentRepository;
import com.abc.studentportal.instructor.application.InstructorRepository;
import com.abc.studentportal.instructor.application.InstructorService;
import com.abc.studentportal.student.application.StudentProfileRepository;
import com.abc.studentportal.student.application.StudentRepository;
import com.abc.studentportal.student.application.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local-dynamodb")
class DynamoDbPropertiesTest {

	@Autowired
	private DynamoDbProperties properties;
	@Autowired
	private DepartmentRepository departments;
	@Autowired
	private StudentRepository students;
	@Autowired
	private StudentProfileRepository profiles;
	@Autowired
	private InstructorRepository instructors;
	@Autowired
	private CourseRepository courses;
	@Autowired
	private EnrollmentRepository enrollments;
	@Autowired
	private DepartmentService departmentService;
	@Autowired
	private StudentService studentService;
	@Autowired
	private InstructorService instructorService;
	@Autowired
	private CourseService courseService;

	@Test
	void bindsLocalDynamoDbDefaults() {
		assertThat(properties.region()).isEqualTo("us-east-1");
		assertThat(properties.endpoint()).isEqualTo(URI.create("http://localhost:4566"));
		assertThat(properties.tables().departments()).isEqualTo("student-portal-departments");
		assertThat(properties.tables().students()).isEqualTo("student-portal-students");
		assertThat(properties.tables().studentProfiles()).isEqualTo("student-portal-student-profiles");
		assertThat(properties.tables().instructors()).isEqualTo("student-portal-instructors");
		assertThat(properties.tables().courses()).isEqualTo("student-portal-courses");
		assertThat(properties.tables().enrollments()).isEqualTo("student-portal-enrollments");
		assertThat(departments).isNotNull();
		assertThat(students).isNotNull();
		assertThat(profiles).isNotNull();
		assertThat(instructors).isNotNull();
		assertThat(courses).isNotNull();
		assertThat(enrollments).isNotNull();
		assertThat(departmentService).isNotNull();
		assertThat(studentService).isNotNull();
		assertThat(instructorService).isNotNull();
		assertThat(courseService).isNotNull();
	}
}
