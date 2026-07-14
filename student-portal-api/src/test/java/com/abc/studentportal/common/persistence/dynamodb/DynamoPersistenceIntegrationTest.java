package com.abc.studentportal.common.persistence.dynamodb;

import com.abc.studentportal.common.exception.ConflictException;
import com.abc.studentportal.common.exception.InvalidRequestException;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.course.domain.Course;
import com.abc.studentportal.course.domain.CourseStatus;
import com.abc.studentportal.course.persistence.dynamodb.CourseDynamoRecord;
import com.abc.studentportal.course.persistence.dynamodb.DynamoCourseRepository;
import com.abc.studentportal.department.domain.Department;
import com.abc.studentportal.department.persistence.dynamodb.DepartmentDynamoRecord;
import com.abc.studentportal.department.persistence.dynamodb.DynamoDepartmentRepository;
import com.abc.studentportal.enrollment.persistence.dynamodb.EnrollmentDynamoRecord;
import com.abc.studentportal.enrollment.persistence.dynamodb.DynamoEnrollmentRepository;
import com.abc.studentportal.enrollment.domain.Enrollment;
import com.abc.studentportal.enrollment.domain.EnrollmentStatus;
import com.abc.studentportal.instructor.domain.Instructor;
import com.abc.studentportal.instructor.persistence.dynamodb.DynamoInstructorRepository;
import com.abc.studentportal.instructor.persistence.dynamodb.InstructorDynamoRecord;
import com.abc.studentportal.student.domain.Student;
import com.abc.studentportal.student.domain.StudentProfile;
import com.abc.studentportal.student.domain.StudentStatus;
import com.abc.studentportal.student.persistence.dynamodb.DynamoStudentProfileRepository;
import com.abc.studentportal.student.persistence.dynamodb.DynamoStudentRepository;
import com.abc.studentportal.student.persistence.dynamodb.StudentDynamoRecord;
import com.abc.studentportal.student.persistence.dynamodb.StudentProfileDynamoRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dynamodb-integration")
@Testcontainers
class DynamoPersistenceIntegrationTest {
	private static final String PREFIX = "test-student-portal";

	@Container
	private static final LocalStackContainer LOCALSTACK = new LocalStackContainer(
			DockerImageName.parse("localstack/localstack:4.14.0")).withServices("dynamodb");

	private static DynamoDbClient client;
	private static DynamoDbTables tables;
	private static DynamoDepartmentRepository departments;
	private static DynamoStudentRepository students;
	private static DynamoStudentProfileRepository profiles;
	private static DynamoInstructorRepository instructors;
	private static DynamoCourseRepository courses;
	private static DynamoEnrollmentRepository enrollments;

	@BeforeAll
	static void setUp() {
		client = DynamoDbClient.builder()
				.endpointOverride(LOCALSTACK.getEndpoint())
				.region(software.amazon.awssdk.regions.Region.of(LOCALSTACK.getRegion()))
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
						LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())))
				.build();

		TABLES.forEach(DynamoPersistenceIntegrationTest::createTable);
		DynamoDbEnhancedClient enhanced = DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
		tables = new DynamoDbTables(
				enhanced.table(name("departments"), TableSchema.fromBean(DepartmentDynamoRecord.class)),
				enhanced.table(name("students"), TableSchema.fromBean(StudentDynamoRecord.class)),
				enhanced.table(name("student-profiles"), TableSchema.fromBean(StudentProfileDynamoRecord.class)),
				enhanced.table(name("instructors"), TableSchema.fromBean(InstructorDynamoRecord.class)),
				enhanced.table(name("courses"), TableSchema.fromBean(CourseDynamoRecord.class)),
				enhanced.table(name("enrollments"), TableSchema.fromBean(EnrollmentDynamoRecord.class)));
		DynamoCursorCodec cursorCodec = new DynamoCursorCodec();
		departments = new DynamoDepartmentRepository(tables, cursorCodec);
		students = new DynamoStudentRepository(tables, cursorCodec);
		profiles = new DynamoStudentProfileRepository(tables);
		instructors = new DynamoInstructorRepository(tables, cursorCodec);
		courses = new DynamoCourseRepository(tables, cursorCodec);
		enrollments = new DynamoEnrollmentRepository(tables, cursorCodec);
	}

	@AfterAll
	static void tearDown() {
		if (client != null) client.close();
	}

	@Test
	void provisionsTheSixTableSchemaWithEveryExpectedIndex() {
		assertEquals(6, TABLES.size());
		TABLES.forEach(spec -> {
			var description = client.describeTable(request -> request.tableName(name(spec.suffix()))).table();
			Set<String> actualIndexes = description.globalSecondaryIndexes().stream()
					.map(index -> index.indexName()).collect(Collectors.toSet());
			assertEquals(spec.indexes().stream().map(IndexSpec::name).collect(Collectors.toSet()), actualIndexes);
			assertEquals(spec.partitionKey(), description.keySchema().getFirst().attributeName());
		});
	}

	@Test
	void supportsCrudSecondaryIndexLookupAndOptimisticConcurrency() {
		Instant createdAt = Instant.parse("2026-01-02T03:04:05Z");
		UUID id = UUID.randomUUID();
		Department candidate = new Department(id, "CS", "Computer Science", null, createdAt, createdAt, 0);

		Department created = departments.create(candidate);
		assertEquals(1, created.version());
		assertEquals(created, departments.findById(id).orElseThrow());
		assertTrue(departments.existsByCode("CS"));
		assertThrows(ConflictException.class, () -> departments.create(candidate));

		Department changed = new Department(id, "CS", "Computing", null, createdAt, createdAt.plusSeconds(1), created.version());
		Department updated = departments.update(changed);
		assertEquals(2, updated.version());
		assertEquals("Computing", departments.findById(id).orElseThrow().name());
		assertThrows(ConflictException.class, () -> departments.update(changed));
		assertThrows(ConflictException.class, () -> departments.delete(created));

		departments.delete(updated);
		assertFalse(departments.findById(id).isPresent());
		assertThrows(ConflictException.class, () -> departments.update(updated));
	}

	@Test
	void persistsEveryDomainRecordAndExposesItsAlternateKeys() {
		Instant now = Instant.parse("2026-02-03T04:05:06Z");
		UUID departmentId = UUID.randomUUID();
		UUID studentId = UUID.randomUUID();
		UUID instructorId = UUID.randomUUID();
		UUID courseId = UUID.randomUUID();

		Department department = departments.create(new Department(departmentId, "MATH", "Mathematics", null,
				now, now, 0));
		Student student = students.create(new Student(studentId, "S-200", "Katherine", "Johnson",
				"KATHERINE@EXAMPLE.COM", StudentStatus.ACTIVE, departmentId, now, now, 0));
		StudentProfile profile = profiles.create(new StudentProfile(UUID.randomUUID(), studentId,
				LocalDate.parse("2001-04-05"), "555-0110", "2 Main St", null, "Austin", "TX", "78701", "US",
				now, now, 0));
		Instructor instructor = instructors.create(new Instructor(instructorId, "E-200", "Alan", "Turing",
				"ALAN@EXAMPLE.COM", departmentId, now, now, 0));
		Course course = courses.create(new Course(courseId, "MATH-101", "Discrete Mathematics", null, 3, 20,
				CourseStatus.OPEN, departmentId, instructorId, now, now, 0));
		Enrollment enrollment = enrollments.create(new Enrollment(UUID.randomUUID(), studentId, courseId,
				EnrollmentStatus.ENROLLED, now, null, null, now, now, 0));

		assertEquals(1, department.version());
		assertEquals(student, students.findById(studentId).orElseThrow());
		assertEquals(profile, profiles.findByStudentId(studentId).orElseThrow());
		assertEquals(instructor, instructors.findById(instructorId).orElseThrow());
		assertEquals(course, courses.findById(courseId).orElseThrow());
		assertEquals(enrollment, enrollments.findById(enrollment.id()).orElseThrow());

		assertTrue(students.existsByStudentNumber("S-200"));
		assertTrue(students.existsByEmail("katherine@example.com"));
		assertTrue(instructors.existsByEmployeeNumber("E-200"));
		assertTrue(instructors.existsByEmail("alan@example.com"));
		assertTrue(courses.existsByCourseCode("MATH-101"));
		assertTrue(enrollments.existsByStudentId(studentId));
		assertTrue(enrollments.existsByCourseId(courseId));
		assertFalse(enrollments.existsActiveByStudentIdAndCourseId(studentId, courseId));
		EnrollmentDynamoRecord activeLock = new EnrollmentDynamoRecord();
		activeLock.setId("ACTIVE#" + studentId + "#" + courseId);
		activeLock.setRecordType("ACTIVE_ENROLLMENT_LOCK");
		tables.enrollments().putItem(activeLock);
		assertTrue(enrollments.existsActiveByStudentIdAndCourseId(studentId, courseId));

		assertTrue(DynamoQueries.exists(tables.departments().index("departments-catalog"), "DEPARTMENT"));
		assertTrue(DynamoQueries.exists(tables.students().index("students-by-department"), departmentId.toString()));
		assertTrue(DynamoQueries.exists(tables.students().index("students-by-status"), StudentStatus.ACTIVE.name()));
		assertTrue(DynamoQueries.exists(tables.students().index("students-catalog"), "STUDENT"));
		assertTrue(DynamoQueries.exists(tables.instructors().index("instructors-by-department"), departmentId.toString()));
		assertTrue(DynamoQueries.exists(tables.instructors().index("instructors-catalog"), "INSTRUCTOR"));
		assertTrue(DynamoQueries.exists(tables.courses().index("courses-by-department"), departmentId.toString()));
		assertTrue(DynamoQueries.exists(tables.courses().index("courses-by-instructor"), instructorId.toString()));
		assertTrue(DynamoQueries.exists(tables.courses().index("courses-by-status"), CourseStatus.OPEN.name()));
		assertTrue(DynamoQueries.exists(tables.courses().index("courses-catalog"), "COURSE"));
		assertTrue(DynamoQueries.exists(tables.enrollments().index("enrollments-by-status"), EnrollmentStatus.ENROLLED.name()));
		assertTrue(DynamoQueries.exists(tables.enrollments().index("enrollments-catalog"), "ENROLLMENT"));
		CursorRequest all = new CursorRequest(100, null);
		assertTrue(departments.findAll(all).content().contains(department));
		assertEquals(List.of(student), students.findByDepartment(departmentId, "john", all).content());
		assertEquals(List.of(student), students.findByStatus(StudentStatus.ACTIVE, all).content());
		var emptyPage = students.findByStatus(StudentStatus.SUSPENDED, all);
		assertTrue(emptyPage.content().isEmpty());
		assertFalse(emptyPage.hasNext());
		assertTrue(students.findAll(all).content().contains(student));
		assertEquals(List.of(instructor), instructors.findByDepartment(departmentId, all).content());
		assertTrue(instructors.findAll(all).content().contains(instructor));
		assertEquals(List.of(course), courses.findByDepartment(departmentId, all).content());
		assertEquals(List.of(course), courses.findByInstructor(instructorId, all).content());
		assertEquals(List.of(course), courses.findByStatus(CourseStatus.OPEN, all).content());
		assertTrue(courses.findAll(all).content().contains(course));
		assertEquals(List.of(enrollment), enrollments.findByStudent(studentId, now, now, all).content());
		assertEquals(List.of(enrollment), enrollments.findByCourse(courseId, null, now, all).content());
		assertEquals(List.of(enrollment), enrollments.findByStatus(EnrollmentStatus.ENROLLED, all).content());
		assertTrue(enrollments.findAll(all).content().contains(enrollment));
		assertThrows(InvalidRequestException.class,
				() -> enrollments.findByStudent(studentId, now.plusSeconds(1), now, all));
		long logicalEnrollmentCount = tables.enrollments().index("enrollments-catalog").query(request -> request
				.queryConditional(software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo(
						software.amazon.awssdk.enhanced.dynamodb.Key.builder().partitionValue("ENROLLMENT").build())))
				.stream().flatMap(page -> page.items().stream()).count();
		assertEquals(1, logicalEnrollmentCount);

		CourseDynamoRecord capacityState = tables.courses().getItem(request -> request.key(key(courseId))
				.consistentRead(true));
		capacityState.setOccupiedSeats(2L);
		tables.courses().updateItem(capacityState);
		Course afterCapacityChange = courses.findById(courseId).orElseThrow();
		Course renamed = new Course(courseId, afterCapacityChange.courseCode(), "Discrete Structures", null,
				afterCapacityChange.credits(), afterCapacityChange.capacity(), afterCapacityChange.status(), departmentId,
				instructorId, now, now.plusSeconds(1), afterCapacityChange.version());
		courses.update(renamed);
		assertEquals(2L, tables.courses().getItem(request -> request.key(key(courseId)).consistentRead(true))
				.getOccupiedSeats());

		Course missing = new Course(UUID.randomUUID(), "MATH-404", "Missing", null, 3, 20, CourseStatus.DRAFT,
				departmentId, instructorId, now, now, 1);
		assertThrows(ConflictException.class, () -> courses.update(missing));
	}

	@Test
	void paginatesWithOpaqueQueryBoundCursorsWithoutDuplicates() {
		Instant base = Instant.parse("2026-03-01T00:00:00Z");
		Set<UUID> inserted = new java.util.HashSet<>();
		for (int index = 0; index < 5; index++) {
			UUID id = UUID.randomUUID();
			inserted.add(id);
			departments.create(new Department(id, "P" + index, "Pagination " + index, null,
					base.plusSeconds(index), base.plusSeconds(index), 0));
		}

		List<UUID> seen = new ArrayList<>();
		String cursor = null;
		do {
			var page = departments.findAll(new CursorRequest(2, cursor));
			assertTrue(page.content().size() <= 2);
			seen.addAll(page.content().stream().map(Department::id).toList());
			cursor = page.nextCursor();
			assertEquals(cursor != null, page.hasNext());
		} while (cursor != null);

		assertTrue(seen.containsAll(inserted));
		assertEquals(seen.size(), Set.copyOf(seen).size());
		String departmentCursor = departments.findAll(new CursorRequest(1, null)).nextCursor();
		assertThrows(InvalidRequestException.class,
				() -> students.findAll(new CursorRequest(1, departmentCursor)));
	}

	private static void createTable(TableSpec spec) {
		Map<String, AttributeDefinition> attributes = new LinkedHashMap<>();
		attributes.put(spec.partitionKey(), attribute(spec.partitionKey()));
		List<GlobalSecondaryIndex> indexes = spec.indexes().stream().map(index -> {
			attributes.put(index.partitionKey(), attribute(index.partitionKey()));
			if (index.sortKey() != null) attributes.put(index.sortKey(), attribute(index.sortKey()));
			return GlobalSecondaryIndex.builder().indexName(index.name())
					.keySchema(keys(index.partitionKey(), index.sortKey()))
					.projection(Projection.builder().projectionType(ProjectionType.ALL).build()).build();
		}).toList();
		CreateTableRequest.Builder request = CreateTableRequest.builder().tableName(name(spec.suffix()))
				.billingMode(BillingMode.PAY_PER_REQUEST).attributeDefinitions(attributes.values())
				.keySchema(keys(spec.partitionKey(), null));
		if (!indexes.isEmpty()) request.globalSecondaryIndexes(indexes);
		client.createTable(request.build());
		client.waiter().waitUntilTableExists(builder -> builder.tableName(name(spec.suffix())));
	}

	private static AttributeDefinition attribute(String name) {
		return AttributeDefinition.builder().attributeName(name).attributeType(ScalarAttributeType.S).build();
	}

	private static List<KeySchemaElement> keys(String partitionKey, String sortKey) {
		var partition = KeySchemaElement.builder().attributeName(partitionKey).keyType(KeyType.HASH).build();
		if (sortKey == null) return List.of(partition);
		return List.of(partition, KeySchemaElement.builder().attributeName(sortKey).keyType(KeyType.RANGE).build());
	}

	private static String name(String suffix) {
		return PREFIX + "-" + suffix;
	}

	private static software.amazon.awssdk.enhanced.dynamodb.Key key(UUID id) {
		return software.amazon.awssdk.enhanced.dynamodb.Key.builder().partitionValue(id.toString()).build();
	}

	private record TableSpec(String suffix, String partitionKey, List<IndexSpec> indexes) {
		private TableSpec(String suffix, IndexSpec... indexes) { this(suffix, "id", Arrays.asList(indexes)); }
		private TableSpec(String suffix, String partitionKey) { this(suffix, partitionKey, List.of()); }
	}

	private record IndexSpec(String name, String partitionKey, String sortKey) {
		private IndexSpec(String name, String partitionKey) { this(name, partitionKey, null); }
	}

	private static final List<TableSpec> TABLES = List.of(
			new TableSpec("departments", new IndexSpec("departments-by-code", "code"),
					new IndexSpec("departments-catalog", "entityType", "createdAtId")),
			new TableSpec("students", new IndexSpec("students-by-number", "studentNumber"),
					new IndexSpec("students-by-email", "email"), new IndexSpec("students-by-department", "departmentId", "lastNameId"),
					new IndexSpec("students-by-status", "status", "updatedAtId"), new IndexSpec("students-catalog", "entityType", "createdAtId")),
			new TableSpec("student-profiles", "studentId"),
			new TableSpec("instructors", new IndexSpec("instructors-by-number", "employeeNumber"),
					new IndexSpec("instructors-by-email", "email"), new IndexSpec("instructors-by-department", "departmentId", "lastNameId"),
					new IndexSpec("instructors-catalog", "entityType", "createdAtId")),
			new TableSpec("courses", new IndexSpec("courses-by-code", "courseCode"),
					new IndexSpec("courses-by-department", "departmentId", "courseCodeId"), new IndexSpec("courses-by-instructor", "instructorId", "courseCodeId"),
					new IndexSpec("courses-by-status", "status", "updatedAtId"), new IndexSpec("courses-catalog", "entityType", "createdAtId")),
			new TableSpec("enrollments", new IndexSpec("enrollments-by-student", "studentId", "enrolledAtId"),
					new IndexSpec("enrollments-by-course", "courseId", "enrolledAtId"), new IndexSpec("enrollments-by-status", "status", "enrolledAtId"),
					new IndexSpec("enrollments-catalog", "entityType", "enrolledAtId")));
}
