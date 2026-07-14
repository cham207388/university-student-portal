package com.abc.studentportal.common.seed;

import com.abc.studentportal.course.application.CourseRepository;
import com.abc.studentportal.course.domain.Course;
import com.abc.studentportal.course.domain.CourseStatus;
import com.abc.studentportal.department.application.DepartmentRepository;
import com.abc.studentportal.department.domain.Department;
import com.abc.studentportal.enrollment.application.EnrollmentRepository;
import com.abc.studentportal.enrollment.domain.Enrollment;
import com.abc.studentportal.enrollment.domain.EnrollmentStatus;
import com.abc.studentportal.enrollment.persistence.dynamodb.DynamoEnrollmentTransactionWriter;
import com.abc.studentportal.instructor.application.InstructorRepository;
import com.abc.studentportal.instructor.domain.Instructor;
import com.abc.studentportal.student.application.StudentProfileRepository;
import com.abc.studentportal.student.application.StudentRepository;
import com.abc.studentportal.student.domain.Student;
import com.abc.studentportal.student.domain.StudentProfile;
import com.abc.studentportal.student.domain.StudentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
@Profile("local-dynamodb")
@ConditionalOnProperty(name = "student-portal.seed.enabled", havingValue = "true")
public class DynamoDevelopmentSeeder implements ApplicationRunner {
	private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDevelopmentSeeder.class);
	private static final Instant CREATED = Instant.parse("2026-01-15T15:00:00Z");
	private final DepartmentRepository departments; private final StudentRepository students;
	private final StudentProfileRepository profiles; private final InstructorRepository instructors;
	private final CourseRepository courses; private final EnrollmentRepository enrollments;
	private final DynamoEnrollmentTransactionWriter enrollmentWriter;

	public DynamoDevelopmentSeeder(DepartmentRepository departments, StudentRepository students,
			StudentProfileRepository profiles, InstructorRepository instructors, CourseRepository courses,
			EnrollmentRepository enrollments, DynamoEnrollmentTransactionWriter enrollmentWriter) {
		this.departments = departments; this.students = students; this.profiles = profiles;
		this.instructors = instructors; this.courses = courses; this.enrollments = enrollments;
		this.enrollmentWriter = enrollmentWriter;
	}

	@Override public void run(ApplicationArguments args) { seed(); }

	public void seed() {
		LOGGER.info("event=development_seed_started database=dynamodb");
		List<Department> departmentData = List.of(
				new Department(id("department-computing"), "CSE", "Computing and Software Engineering", "Software and systems", CREATED, CREATED, 0),
				new Department(id("department-sciences"), "SCI", "Natural Sciences", "Mathematics and laboratory sciences", CREATED.plusSeconds(1), CREATED.plusSeconds(1), 0),
				new Department(id("department-humanities"), "HUM", "Humanities", "History, writing, and culture", CREATED.plusSeconds(2), CREATED.plusSeconds(2), 0));
		departmentData.forEach(value -> { if (departments.findById(value.id()).isEmpty()) departments.create(value); });

		List<Instructor> instructorData = List.of(
				instructor(1, "E1001", "Mira", "Chen", departmentData.get(0).id()), instructor(2, "E1002", "Jonah", "Reed", departmentData.get(0).id()),
				instructor(3, "E1003", "Priya", "Nair", departmentData.get(1).id()), instructor(4, "E1004", "Mateo", "Silva", departmentData.get(1).id()),
				instructor(5, "E1005", "Elena", "Brooks", departmentData.get(2).id()));
		instructorData.forEach(value -> { if (instructors.findById(value.id()).isEmpty()) instructors.create(value); });

		String[][] names = {{"Avery","Morgan"},{"Noah","Williams"},{"Sofia","Patel"},{"Liam","Kim"},{"Zoe","Martinez"},
				{"Ethan","Nguyen"},{"Maya","Johnson"},{"Lucas","Garcia"},{"Amara","Wilson"},{"Owen","Davis"}};
		java.util.ArrayList<Student> studentData = new java.util.ArrayList<>();
		for (int index = 0; index < names.length; index++) {
			UUID departmentId = departmentData.get(index % departmentData.size()).id();
			Student value = new Student(id("student-" + (index + 1)), "S" + String.format("%04d", index + 1), names[index][0], names[index][1],
					"student" + (index + 1) + "@example.edu", StudentStatus.ACTIVE, departmentId,
					CREATED.plusSeconds(20 + index), CREATED.plusSeconds(20 + index), 0);
			studentData.add(value); if (students.findById(value.id()).isEmpty()) students.create(value);
			if (profiles.findByStudentId(value.id()).isEmpty()) profiles.create(profile(value, index));
		}

		CourseStatus[] targets = {CourseStatus.OPEN, CourseStatus.OPEN, CourseStatus.DRAFT, CourseStatus.CLOSED,
				CourseStatus.COMPLETED, CourseStatus.OPEN, CourseStatus.CANCELLED, CourseStatus.OPEN, CourseStatus.DRAFT, CourseStatus.OPEN};
		java.util.ArrayList<Course> courseData = new java.util.ArrayList<>();
		for (int index = 0; index < 10; index++) {
			Instructor instructor = instructorData.get(index % instructorData.size());
			CourseStatus initialStatus = targets[index] == CourseStatus.DRAFT || targets[index] == CourseStatus.CANCELLED
					? targets[index] : CourseStatus.OPEN;
			Course value = new Course(id("course-" + (index + 1)), "CSE-" + (101 + index), "University Course " + (index + 1),
					"Fictional development course", 3 + index % 2, index == 0 ? 2 : 20, CourseStatus.OPEN,
					instructor.departmentId(), instructor.id(), CREATED.plusSeconds(50 + index), CREATED.plusSeconds(50 + index), 0);
			value = new Course(value.id(), value.courseCode(), value.title(), value.description(), value.credits(), value.capacity(),
					initialStatus, value.departmentId(), value.instructorId(), value.createdAt(), value.updatedAt(), value.version());
			courseData.add(value); if (courses.findById(value.id()).isEmpty()) courses.create(value);
		}

		ensureEnrollment(1, studentData.get(0), courseData.get(0), EnrollmentStatus.ENROLLED);
		ensureEnrollment(2, studentData.get(1), courseData.get(0), EnrollmentStatus.COMPLETED);
		ensureEnrollment(3, studentData.get(2), courseData.get(1), EnrollmentStatus.WAITLISTED);
		ensureEnrollment(4, studentData.get(3), courseData.get(1), EnrollmentStatus.DROPPED);
		ensureEnrollment(5, studentData.get(4), courseData.get(5), EnrollmentStatus.ENROLLED);
		ensureEnrollment(6, studentData.get(5), courseData.get(7), EnrollmentStatus.COMPLETED);

		for (int index = 0; index < targets.length; index++) setCourseStatus(courseData.get(index).id(), targets[index]);
		setStudentStatus(studentData.get(8).id(), StudentStatus.GRADUATED);
		setStudentStatus(studentData.get(9).id(), StudentStatus.INACTIVE);
		LOGGER.info("event=development_seed_completed departments=3 students=10 profiles=10 instructors=5 courses=10 enrollments=6");
	}

	private void ensureEnrollment(int number, Student student, Course course, EnrollmentStatus target) {
		UUID enrollmentId = id("enrollment-" + number); Enrollment current = enrollments.findById(enrollmentId).orElse(null);
		if (current == null) {
			Instant time = CREATED.plusSeconds(100 + number);
			EnrollmentStatus initial = target == EnrollmentStatus.WAITLISTED ? EnrollmentStatus.WAITLISTED : EnrollmentStatus.ENROLLED;
			current = enrollments.create(new Enrollment(enrollmentId, student.id(), course.id(), initial, time, null, null, time, time, 0));
		}
		if (current.status() != target) {
			Enrollment changed = current.transitionTo(target, target == EnrollmentStatus.COMPLETED ? "A" : null,
					CREATED.plusSeconds(200 + number));
			current = enrollments.update(changed);
		}
		enrollmentWriter.ensureRelationship(current);
	}

	private void setCourseStatus(UUID id, CourseStatus status) {
		Course current = courses.findById(id).orElseThrow();
		if (current.status() == status) return;
		Course changed = current.transitionTo(status, CREATED.plusSeconds(300));
		courses.update(new Course(changed.id(), changed.courseCode(), changed.title(), changed.description(), changed.credits(),
				changed.capacity(), changed.status(), changed.departmentId(), changed.instructorId(), changed.createdAt(), changed.updatedAt(), current.version()));
	}

	private void setStudentStatus(UUID id, StudentStatus status) {
		Student current = students.findById(id).orElseThrow(); if (current.status() == status) return;
		Student changed = current.changeStatus(status, CREATED.plusSeconds(301));
		students.update(new Student(changed.id(), changed.studentNumber(), changed.firstName(), changed.lastName(), changed.email(),
				changed.status(), changed.departmentId(), changed.createdAt(), changed.updatedAt(), current.version()));
	}

	private static Instructor instructor(int number, String employee, String first, String last, UUID departmentId) {
		return new Instructor(id("instructor-" + number), employee, first, last, first.toLowerCase() + "." + last.toLowerCase() + "@example.edu",
				departmentId, CREATED.plusSeconds(10 + number), CREATED.plusSeconds(10 + number), 0);
	}
	private static StudentProfile profile(Student student, int index) {
		return new StudentProfile(id("profile-" + (index + 1)), student.id(), LocalDate.of(1998 + index % 5, 1 + index % 12, 10 + index),
				"555-01" + String.format("%02d", index), (100 + index) + " University Way", null, "Austin", "TX", "78701", "US",
				CREATED.plusSeconds(40 + index), CREATED.plusSeconds(40 + index), 0);
	}
	private static UUID id(String name) { return UUID.nameUUIDFromBytes(("student-portal:" + name).getBytes(StandardCharsets.UTF_8)); }
}
