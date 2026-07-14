package com.abc.studentportal.common.persistence.dynamodb;

import com.abc.studentportal.course.domain.Course;
import com.abc.studentportal.course.domain.CourseStatus;
import com.abc.studentportal.course.persistence.dynamodb.CourseDynamoMapper;
import com.abc.studentportal.department.domain.Department;
import com.abc.studentportal.department.persistence.dynamodb.DepartmentDynamoMapper;
import com.abc.studentportal.enrollment.domain.Enrollment;
import com.abc.studentportal.enrollment.domain.EnrollmentStatus;
import com.abc.studentportal.enrollment.persistence.dynamodb.EnrollmentDynamoMapper;
import com.abc.studentportal.instructor.domain.Instructor;
import com.abc.studentportal.instructor.persistence.dynamodb.InstructorDynamoMapper;
import com.abc.studentportal.student.domain.Student;
import com.abc.studentportal.student.domain.StudentProfile;
import com.abc.studentportal.student.domain.StudentStatus;
import com.abc.studentportal.student.persistence.dynamodb.StudentDynamoMapper;
import com.abc.studentportal.student.persistence.dynamodb.StudentProfileDynamoMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DynamoMapperTest {
	private static final Instant CREATED = Instant.parse("2026-01-02T03:04:05Z");
	private static final Instant UPDATED = Instant.parse("2026-01-03T04:05:06Z");
	private static final UUID DEPARTMENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID STUDENT_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
	private static final UUID INSTRUCTOR_ID = UUID.fromString("30000000-0000-0000-0000-000000000003");
	private static final UUID COURSE_ID = UUID.fromString("40000000-0000-0000-0000-000000000004");
	private static final UUID ENROLLMENT_ID = UUID.fromString("50000000-0000-0000-0000-000000000005");

	@Test
	void roundTripsDepartmentAndBuildsCatalogKeys() {
		Department domain = new Department(DEPARTMENT_ID, "cs", "Computing", null, CREATED, UPDATED, 7);
		var record = DepartmentDynamoMapper.toRecord(domain);

		assertEquals("DEPARTMENT", record.getEntityType());
		assertEquals(CREATED + "#" + DEPARTMENT_ID, record.getCreatedAtId());
		assertNull(record.getDescription());
		assertEquals(domain, DepartmentDynamoMapper.toDomain(record));
	}

	@Test
	void roundTripsStudentAndBuildsEveryIndexKey() {
		Student domain = new Student(STUDENT_ID, "S-100", "Ada", "lovelace", "ADA@EXAMPLE.COM",
				StudentStatus.ACTIVE, DEPARTMENT_ID, CREATED, UPDATED, 7);
		var record = StudentDynamoMapper.toRecord(domain);

		assertEquals("STUDENT", record.getEntityType());
		assertEquals(CREATED + "#" + STUDENT_ID, record.getCreatedAtId());
		assertEquals(UPDATED + "#" + STUDENT_ID, record.getUpdatedAtId());
		assertEquals("LOVELACE#" + STUDENT_ID, record.getLastNameId());
		assertEquals("ada@example.com", record.getEmail());
		assertEquals(domain, StudentDynamoMapper.toDomain(record));
	}

	@Test
	void roundTripsStudentProfileWithStudentPartitionKey() {
		StudentProfile domain = new StudentProfile(UUID.randomUUID(), STUDENT_ID, LocalDate.parse("2000-02-03"),
				"555-0100", "1 Main St", null, "Austin", "TX", "78701", "US", CREATED, UPDATED, 7);
		var record = StudentProfileDynamoMapper.toRecord(domain);

		assertEquals(STUDENT_ID.toString(), record.getStudentId());
		assertNull(record.getAddressLine2());
		assertEquals(domain, StudentProfileDynamoMapper.toDomain(record));
	}

	@Test
	void roundTripsInstructorAndBuildsEveryIndexKey() {
		Instructor domain = new Instructor(INSTRUCTOR_ID, "E-100", "Grace", "hopper", "GRACE@EXAMPLE.COM",
				DEPARTMENT_ID, CREATED, UPDATED, 7);
		var record = InstructorDynamoMapper.toRecord(domain);

		assertEquals("INSTRUCTOR", record.getEntityType());
		assertEquals(CREATED + "#" + INSTRUCTOR_ID, record.getCreatedAtId());
		assertEquals("HOPPER#" + INSTRUCTOR_ID, record.getLastNameId());
		assertEquals("grace@example.com", record.getEmail());
		assertEquals(domain, InstructorDynamoMapper.toDomain(record));
	}

	@Test
	void roundTripsCourseAndInitializesCapacityState() {
		Course domain = new Course(COURSE_ID, "cs-101", "Foundations", null, 3, 25, CourseStatus.OPEN,
				DEPARTMENT_ID, INSTRUCTOR_ID, CREATED, UPDATED, 7);
		var record = CourseDynamoMapper.toRecord(domain);

		assertEquals("COURSE", record.getEntityType());
		assertEquals(0L, record.getOccupiedSeats());
		assertEquals(CREATED + "#" + COURSE_ID, record.getCreatedAtId());
		assertEquals(UPDATED + "#" + COURSE_ID, record.getUpdatedAtId());
		assertEquals("CS-101#" + COURSE_ID, record.getCourseCodeId());
		assertEquals(domain, CourseDynamoMapper.toDomain(record));
	}

	@Test
	void roundTripsEnrollmentAndBuildsSparseAuthoritativeKeys() {
		Enrollment domain = new Enrollment(ENROLLMENT_ID, STUDENT_ID, COURSE_ID, EnrollmentStatus.ENROLLED,
				CREATED, null, null, CREATED, UPDATED, 7);
		var record = EnrollmentDynamoMapper.toRecord(domain);

		assertEquals("ENROLLMENT", record.getRecordType());
		assertEquals("ENROLLMENT", record.getEntityType());
		assertEquals(CREATED + "#" + ENROLLMENT_ID, record.getEnrolledAtId());
		assertNull(record.getDroppedAt());
		assertNull(record.getFinalGrade());
		assertEquals(domain, EnrollmentDynamoMapper.toDomain(record));
	}
}
