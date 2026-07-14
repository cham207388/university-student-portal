package com.abc.studentportal.common.persistence.dynamodb;

import com.abc.studentportal.StudentPortalApiApplication;
import com.abc.studentportal.common.exception.ConflictException;
import com.abc.studentportal.common.exception.InvalidRequestException;
import com.abc.studentportal.common.application.DependencyChecker;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.common.seed.DynamoDevelopmentSeeder;
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
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
	private static com.abc.studentportal.enrollment.persistence.dynamodb.DynamoEnrollmentTransactionWriter enrollmentWriter;
	private static DynamoStudentCourseQueryService relationships;
	private static DependencyChecker dependencies;
	private static ConfigurableApplicationContext application;
	private static MockMvc mvc;

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
		DynamoTransactionalWriter writer = new DynamoTransactionalWriter(client);
		DynamoRelationshipCounters relationshipCounters = new DynamoRelationshipCounters(tables);
		departments = new DynamoDepartmentRepository(tables, cursorCodec, writer);
		students = new DynamoStudentRepository(tables, cursorCodec, writer, relationshipCounters);
		profiles = new DynamoStudentProfileRepository(tables);
		instructors = new DynamoInstructorRepository(tables, cursorCodec, writer, relationshipCounters);
		courses = new DynamoCourseRepository(tables, cursorCodec, writer, relationshipCounters);
		enrollmentWriter = new com.abc.studentportal.enrollment.persistence.dynamodb.DynamoEnrollmentTransactionWriter(client, tables);
		enrollments = new DynamoEnrollmentRepository(tables, cursorCodec, enrollmentWriter);
		relationships = new DynamoStudentCourseQueryService(client, tables, cursorCodec);
		dependencies = new DynamoDependencyChecker(students, instructors, courses, enrollments);
		application = new SpringApplicationBuilder(StudentPortalApiApplication.class)
				.profiles("test-dynamodb").web(WebApplicationType.SERVLET)
				.properties(Map.ofEntries(
						Map.entry("server.port", "0"), Map.entry("spring.main.banner-mode", "off"),
						Map.entry("logging.level.root", "WARN"),
						Map.entry("student-portal.seed.enabled", "false"),
						Map.entry("student-portal.dynamodb.region", LOCALSTACK.getRegion()),
						Map.entry("student-portal.dynamodb.endpoint", LOCALSTACK.getEndpoint().toString()),
						Map.entry("student-portal.dynamodb.tables.departments", name("departments")),
						Map.entry("student-portal.dynamodb.tables.students", name("students")),
						Map.entry("student-portal.dynamodb.tables.student-profiles", name("student-profiles")),
						Map.entry("student-portal.dynamodb.tables.instructors", name("instructors")),
						Map.entry("student-portal.dynamodb.tables.courses", name("courses")),
						Map.entry("student-portal.dynamodb.tables.enrollments", name("enrollments"))))
				.run();
		mvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) application)
				.addFilters(application.getBean(com.abc.studentportal.common.observability.CorrelationIdFilter.class))
				.build();
	}

	@AfterAll
	static void tearDown() {
		if (application != null) application.close();
		if (client != null) client.close();
	}

	@Test
	void exercisesRestEndpointsOpenApiAndHealthAgainstLocalStack() throws Exception {
		String code = "API" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
		mvc.perform(post("/api/v1/departments").contentType("application/json")
				.content("{\"code\":\"" + code + "\",\"name\":\"HTTP Integration\"}"))
				.andExpect(status().isCreated()).andExpect(header().exists("Location"))
				.andExpect(header().exists("X-Correlation-ID"));
		mvc.perform(get("/api/v1/departments").param("code", code).header("X-Correlation-ID", "integration-1"))
				.andExpect(status().isOk()).andExpect(header().string("X-Correlation-ID", "integration-1"))
				.andExpect(jsonPath("$.content[0].code").value(code));
		mvc.perform(get("/api/v1/courses").param("title", "ignored"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.correlationId").exists());
		mvc.perform(get("/actuator/health")).andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
		mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("University Student Portal API"))
				.andExpect(jsonPath("$.paths['/api/v1/students'].get.description").isNotEmpty())
				.andExpect(jsonPath("$.paths['/api/v1/students'].get.parameters[?(@.name == 'cursor')].description")
						.isNotEmpty())
				.andExpect(jsonPath("$.paths['/api/v1/students'].get.responses['400'].content['application/problem+json'].schema['$ref']")
						.value("#/components/schemas/ProblemDetail"))
				.andExpect(jsonPath("$.components.schemas.ProblemDetail.properties.fieldErrors").exists());
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
		assertEquals(created, departments.findByCode("CS").orElseThrow());
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
		student = students.findById(studentId).orElseThrow();
		course = courses.findById(courseId).orElseThrow();
		department = departments.findById(departmentId).orElseThrow();
		instructor = instructors.findById(instructorId).orElseThrow();

		assertTrue(department.version() > 1);
		assertEquals(student, students.findById(studentId).orElseThrow());
		assertEquals(profile, profiles.findByStudentId(studentId).orElseThrow());
		assertEquals(instructor, instructors.findById(instructorId).orElseThrow());
		assertEquals(course.id(), courses.findById(courseId).orElseThrow().id());
		assertEquals(enrollment, enrollments.findById(enrollment.id()).orElseThrow());

		assertTrue(students.existsByStudentNumber("S-200"));
		assertTrue(students.existsByEmail("katherine@example.com"));
		assertTrue(instructors.existsByEmployeeNumber("E-200"));
		assertTrue(instructors.existsByEmail("alan@example.com"));
		assertTrue(courses.existsByCourseCode("MATH-101"));
		assertEquals(student, students.findByStudentNumber("S-200").orElseThrow());
		assertEquals(student, students.findByEmail("katherine@example.com").orElseThrow());
		assertEquals(instructor, instructors.findByEmployeeNumber("E-200").orElseThrow());
		assertEquals(instructor, instructors.findByEmail("alan@example.com").orElseThrow());
		assertEquals(course.id(), courses.findByCourseCode("MATH-101").orElseThrow().id());
		assertTrue(enrollments.existsByStudentId(studentId));
		assertTrue(enrollments.existsByCourseId(courseId));
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
		assertTrue(students.findByStatus(StudentStatus.ACTIVE, all).content().contains(student));
		var emptyPage = students.findByStatus(StudentStatus.SUSPENDED, all);
		assertTrue(emptyPage.content().isEmpty());
		assertFalse(emptyPage.hasNext());
		assertTrue(students.findAll(all).content().contains(student));
		assertEquals(List.of(instructor), instructors.findByDepartment(departmentId, all).content());
		assertTrue(instructors.findAll(all).content().contains(instructor));
		assertEquals(List.of(course), courses.findByDepartment(departmentId, all).content());
		assertEquals(List.of(course), courses.findByInstructor(instructorId, all).content());
		assertTrue(courses.findByStatus(CourseStatus.OPEN, all).content().contains(course));
		assertTrue(courses.findAll(all).content().contains(course));
		assertEquals(List.of(enrollment), enrollments.findByStudent(studentId, now, now, all).content());
		assertEquals(List.of(enrollment), enrollments.findByCourse(courseId, null, now, all).content());
		assertTrue(enrollments.findByStatus(EnrollmentStatus.ENROLLED, all).content().contains(enrollment));
		assertTrue(enrollments.findAll(all).content().contains(enrollment));
		assertThrows(InvalidRequestException.class,
				() -> enrollments.findByStudent(studentId, now.plusSeconds(1), now, all));
		long logicalEnrollmentCount = tables.enrollments().index("enrollments-catalog").query(request -> request
				.queryConditional(software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.keyEqualTo(
						software.amazon.awssdk.enhanced.dynamodb.Key.builder().partitionValue("ENROLLMENT").build())))
				.stream().flatMap(page -> page.items().stream()).count();
		assertEquals(enrollments.findAll(all).content().size(), logicalEnrollmentCount);

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

	@Test
	void enforcesAndReleasesAlternateKeysAtomicallyIncludingConcurrentRaces() throws Exception {
		Instant now = Instant.parse("2026-04-01T00:00:00Z");
		Department first = departments.create(new Department(UUID.randomUUID(), "UQ1", "First", null, now, now, 0));
		Department duplicate = new Department(UUID.randomUUID(), "UQ1", "Duplicate", null, now, now, 0);
		assertThrows(ConflictException.class, () -> departments.create(duplicate));
		assertTrue(departments.findById(duplicate.id()).isEmpty());

		Department second = departments.create(new Department(UUID.randomUUID(), "UQ2", "Second", null, now, now, 0));
		Department conflictingUpdate = new Department(first.id(), "UQ2", "Changed", null, now, now.plusSeconds(1),
				first.version());
		assertThrows(ConflictException.class, () -> departments.update(conflictingUpdate));
		assertEquals("UQ1", departments.findById(first.id()).orElseThrow().code());

		Department moved = departments.update(new Department(first.id(), "UQ3", "Moved", null, now,
				now.plusSeconds(2), first.version()));
		Department reusedOld = departments.create(new Department(UUID.randomUUID(), "UQ1", "Reused", null, now, now, 0));
		assertEquals("UQ1", reusedOld.code());
		departments.delete(new Department(moved.id(), "CALLER-VALUE-IS-NOT-AUTHORITY", moved.name(), null,
				moved.createdAt(), moved.updatedAt(), moved.version()));
		assertEquals("UQ3", departments.create(new Department(UUID.randomUUID(), "UQ3", "After delete", null,
				now, now, 0)).code());
		assertEquals("UQ2", second.code());

		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger successes = new AtomicInteger();
		AtomicInteger conflicts = new AtomicInteger();
		try (var executor = Executors.newFixedThreadPool(2)) {
			var tasks = java.util.stream.IntStream.range(0, 2).mapToObj(index -> executor.submit(() -> {
				start.await();
				try {
					departments.create(new Department(UUID.randomUUID(), "RACE", "Race " + index, null, now, now, 0));
					successes.incrementAndGet();
				} catch (ConflictException exception) {
					conflicts.incrementAndGet();
				}
				return null;
			})).toList();
			start.countDown();
			for (var task : tasks) task.get();
		}
		assertEquals(1, successes.get());
		assertEquals(1, conflicts.get());
	}

	@Test
	void enforcesEnrollmentCapacityLocksTransitionsAndConcurrentRollback() throws Exception {
		Instant now = Instant.parse("2026-06-01T00:00:00Z");
		String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase(java.util.Locale.ROOT);
		Department department = departments.create(new Department(UUID.randomUUID(), "D" + suffix, "Enrollment", null,
				now, now, 0));
		Instructor instructor = instructors.create(new Instructor(UUID.randomUUID(), "E" + suffix, "Test", "Teacher",
				"teacher-" + suffix.toLowerCase(java.util.Locale.ROOT) + "@example.com", department.id(), now, now, 0));
		Course course = courses.create(new Course(UUID.randomUUID(), "C" + suffix, "Capacity", null, 3, 1,
				CourseStatus.OPEN, department.id(), instructor.id(), now, now, 0));
		Student firstStudent = createStudent("A" + suffix, department.id(), now);
		Student secondStudent = createStudent("B" + suffix, department.id(), now);

		Enrollment first = enrollments.create(new Enrollment(UUID.randomUUID(), firstStudent.id(), course.id(),
				EnrollmentStatus.ENROLLED, now, null, null, now, now, 0));
		assertEquals(1L, occupiedSeats(course.id()));
		assertTrue(enrollments.existsActiveByStudentIdAndCourseId(firstStudent.id(), course.id()));
		assertTrue(dependencies.departmentHasDependents(department.id()));
		assertTrue(dependencies.studentHasEnrollmentHistory(firstStudent.id()));
		assertTrue(dependencies.instructorHasCourses(instructor.id()));
		assertTrue(dependencies.courseHasEnrollmentHistory(course.id()));
		assertThrows(ConflictException.class, () -> students.delete(students.findById(firstStudent.id()).orElseThrow()));
		assertThrows(ConflictException.class, () -> courses.delete(courses.findById(course.id()).orElseThrow()));
		assertThrows(ConflictException.class, () -> enrollments.create(new Enrollment(UUID.randomUUID(), firstStudent.id(),
				course.id(), EnrollmentStatus.ENROLLED, now, null, null, now, now, 0)));
		assertThrows(ConflictException.class, () -> enrollments.create(new Enrollment(UUID.randomUUID(), secondStudent.id(),
				course.id(), EnrollmentStatus.ENROLLED, now, null, null, now, now, 0)));
		assertEquals(1L, occupiedSeats(course.id()));

		Enrollment droppedCandidate = first.transitionTo(EnrollmentStatus.DROPPED, null, now.plusSeconds(1));
		Enrollment dropped = enrollments.update(droppedCandidate);
		assertEquals(0L, occupiedSeats(course.id()));
		assertFalse(enrollments.existsActiveByStudentIdAndCourseId(firstStudent.id(), course.id()));
		assertThrows(ConflictException.class, () -> enrollments.update(droppedCandidate));

		Enrollment second = enrollments.create(new Enrollment(UUID.randomUUID(), secondStudent.id(), course.id(),
				EnrollmentStatus.ENROLLED, now.plusSeconds(2), null, null, now.plusSeconds(2), now.plusSeconds(2), 0));
		Enrollment completed = enrollments.update(second.transitionTo(EnrollmentStatus.COMPLETED, "A", now.plusSeconds(3)));
		assertEquals(1L, occupiedSeats(course.id()));
		assertFalse(enrollments.existsActiveByStudentIdAndCourseId(secondStudent.id(), course.id()));
		assertEquals(EnrollmentStatus.COMPLETED, completed.status());
		assertEquals(EnrollmentStatus.DROPPED, dropped.status());

		Course raceCourse = courses.create(new Course(UUID.randomUUID(), "R" + suffix, "Race capacity", null, 3, 1,
				CourseStatus.OPEN, department.id(), instructor.id(), now, now, 0));
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger successes = new AtomicInteger();
		AtomicInteger conflicts = new AtomicInteger();
		try (var executor = Executors.newFixedThreadPool(2)) {
			var candidates = List.of(firstStudent, secondStudent);
			var tasks = candidates.stream().map(student -> executor.submit(() -> {
				start.await();
				try {
					enrollments.create(new Enrollment(UUID.randomUUID(), student.id(), raceCourse.id(),
							EnrollmentStatus.ENROLLED, now, null, null, now, now, 0));
					successes.incrementAndGet();
				} catch (ConflictException exception) { conflicts.incrementAndGet(); }
				return null;
			})).toList();
			start.countDown();
			for (var task : tasks) task.get();
		}
		assertEquals(1, successes.get());
		assertEquals(1, conflicts.get());
		assertEquals(1L, occupiedSeats(raceCourse.id()));
	}

	@Test
	void deletesStudentAndProfileAtomicallyAndReleasesStudentClaims() {
		Instant now = Instant.parse("2026-07-01T00:00:00Z");
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		Department department = departments.create(new Department(UUID.randomUUID(), "X" + suffix, "Delete", null,
				now, now, 0));
		Student student = createStudent("DELETE-" + suffix, department.id(), now);
		StudentProfile profile = profiles.create(new StudentProfile(UUID.randomUUID(), student.id(),
				LocalDate.parse("2000-01-01"), "555-0123", "3 Main", null, "Austin", "TX", "78701", "US",
				now, now, 0));

		students.delete(student);
		assertTrue(students.findById(student.id()).isEmpty());
		assertTrue(profiles.findByStudentId(student.id()).isEmpty());
		Student reused = students.create(new Student(UUID.randomUUID(), student.studentNumber(), "Reuse", "Claims",
				student.email(), StudentStatus.ACTIVE, department.id(), now, now, 0));
		assertEquals(student.studentNumber(), reused.studentNumber());
		assertEquals(1, profile.version());
	}

	@Test
	void transfersRelationshipCountersOnMovesAndReleasesParentsOnDelete() {
		Instant now = Instant.parse("2026-08-01T00:00:00Z");
		String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase(java.util.Locale.ROOT);
		Department firstDepartment = departments.create(new Department(UUID.randomUUID(), "MA" + suffix, "Move A", null,
				now, now, 0));
		Department secondDepartment = departments.create(new Department(UUID.randomUUID(), "MB" + suffix, "Move B", null,
				now, now, 0));
		Instructor firstInstructor = instructors.create(new Instructor(UUID.randomUUID(), "IA" + suffix, "First", "Teacher",
				"ia-" + suffix.toLowerCase(java.util.Locale.ROOT) + "@example.com", firstDepartment.id(), now, now, 0));
		Instructor secondInstructor = instructors.create(new Instructor(UUID.randomUUID(), "IB" + suffix, "Second", "Teacher",
				"ib-" + suffix.toLowerCase(java.util.Locale.ROOT) + "@example.com", secondDepartment.id(), now, now, 0));
		Student student = createStudent("MS" + suffix, firstDepartment.id(), now);
		Course course = courses.create(new Course(UUID.randomUUID(), "MC" + suffix, "Moving", null, 3, 10,
				CourseStatus.DRAFT, firstDepartment.id(), firstInstructor.id(), now, now, 0));

		assertDepartmentCounts(firstDepartment.id(), 1, 1, 1);
		assertDepartmentCounts(secondDepartment.id(), 0, 1, 0);
		assertInstructorCourseCount(firstInstructor.id(), 1);
		Student movedStudent = students.update(new Student(student.id(), student.studentNumber(), student.firstName(),
				student.lastName(), student.email(), student.status(), secondDepartment.id(), student.createdAt(),
				now.plusSeconds(1), student.version()));
		Course movedCourse = courses.update(new Course(course.id(), course.courseCode(), course.title(), course.description(),
				course.credits(), course.capacity(), course.status(), secondDepartment.id(), secondInstructor.id(),
				course.createdAt(), now.plusSeconds(1), course.version()));

		assertDepartmentCounts(firstDepartment.id(), 0, 1, 0);
		assertDepartmentCounts(secondDepartment.id(), 1, 1, 1);
		assertInstructorCourseCount(firstInstructor.id(), 0);
		assertInstructorCourseCount(secondInstructor.id(), 1);
		students.delete(movedStudent);
		courses.delete(movedCourse);
		assertDepartmentCounts(secondDepartment.id(), 0, 1, 0);
		assertInstructorCourseCount(secondInstructor.id(), 0);
		instructors.delete(instructors.findById(firstInstructor.id()).orElseThrow());
		instructors.delete(instructors.findById(secondInstructor.id()).orElseThrow());
		departments.delete(departments.findById(firstDepartment.id()).orElseThrow());
		departments.delete(departments.findById(secondDepartment.id()).orElseThrow());
	}

	@Test
	void serializesDepartmentDeleteAgainstStudentCreateAndInstructorDeleteAgainstCourseCreate() throws Exception {
		Instant now = Instant.parse("2026-09-01T00:00:00Z");
		String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase(java.util.Locale.ROOT);
		Department department = departments.create(new Department(UUID.randomUUID(), "RD" + suffix, "Race delete", null,
				now, now, 0));
		AtomicInteger departmentWins = race(
				() -> departments.delete(department),
				() -> createStudent("RS" + suffix, department.id(), now));
		assertEquals(1, departmentWins.get());

		Department courseDepartment = departments.create(new Department(UUID.randomUUID(), "RC" + suffix, "Race course", null,
				now, now, 0));
		Instructor instructor = instructors.create(new Instructor(UUID.randomUUID(), "RI" + suffix, "Race", "Teacher",
				"ri-" + suffix.toLowerCase(java.util.Locale.ROOT) + "@example.com", courseDepartment.id(), now, now, 0));
		AtomicInteger instructorWins = race(
				() -> instructors.delete(instructor),
				() -> courses.create(new Course(UUID.randomUUID(), "RX" + suffix, "Race", null, 3, 5,
						CourseStatus.DRAFT, courseDepartment.id(), instructor.id(), now, now, 0)));
		assertEquals(1, instructorWins.get());
	}

	@Test
	void developmentSeedIsCompleteAndIdempotent() {
		DynamoDevelopmentSeeder seeder = new DynamoDevelopmentSeeder(departments, students, profiles, instructors, courses,
				enrollments, enrollmentWriter);
		seeder.seed();
		seeder.seed();

		assertEquals("CSE", departments.findById(seedId("department-computing")).orElseThrow().code());
		for (int index = 1; index <= 10; index++) {
			UUID studentId = seedId("student-" + index);
			assertTrue(students.findById(studentId).isPresent());
			assertTrue(profiles.findByStudentId(studentId).isPresent());
			assertTrue(courses.findById(seedId("course-" + index)).isPresent());
		}
		for (int index = 1; index <= 5; index++) assertTrue(instructors.findById(seedId("instructor-" + index)).isPresent());
		for (int index = 1; index <= 6; index++) assertTrue(enrollments.findById(seedId("enrollment-" + index)).isPresent());
		assertEquals(2L, occupiedSeats(seedId("course-1")));
		assertEquals(CourseStatus.CANCELLED, courses.findById(seedId("course-7")).orElseThrow().status());
		assertEquals(StudentStatus.GRADUATED, students.findById(seedId("student-9")).orElseThrow().status());
	}

	@Test
	void materializesDeduplicatedStudentCourseRelationshipsWithStableCursors() {
		Instant now = Instant.parse("2026-10-01T00:00:00Z");
		String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase(java.util.Locale.ROOT);
		Department department = departments.create(new Department(UUID.randomUUID(), "ER" + suffix, "Edge relationships", null,
				now, now, 0));
		Instructor instructor = instructors.create(new Instructor(UUID.randomUUID(), "EI" + suffix, "Edge", "Teacher",
				"edge-" + suffix.toLowerCase(java.util.Locale.ROOT) + "@example.com", department.id(), now, now, 0));
		Student first = createStudent("ES" + suffix + "A", department.id(), now);
		Student second = createStudent("ES" + suffix + "B", department.id(), now);
		Course firstCourse = courses.create(new Course(UUID.randomUUID(), "EC" + suffix + "A", "Edge A", null, 3, 5,
				CourseStatus.OPEN, department.id(), instructor.id(), now, now, 0));
		Course secondCourse = courses.create(new Course(UUID.randomUUID(), "EC" + suffix + "B", "Edge B", null, 3, 5,
				CourseStatus.OPEN, department.id(), instructor.id(), now, now, 0));
		Enrollment original = enrollments.create(new Enrollment(UUID.randomUUID(), first.id(), firstCourse.id(),
				EnrollmentStatus.ENROLLED, now, null, null, now, now, 0));
		enrollments.update(original.transitionTo(EnrollmentStatus.DROPPED, null, now.plusSeconds(1)));
		enrollments.create(new Enrollment(UUID.randomUUID(), first.id(), firstCourse.id(), EnrollmentStatus.ENROLLED,
				now.plusSeconds(2), null, null, now.plusSeconds(2), now.plusSeconds(2), 0));
		enrollments.create(new Enrollment(UUID.randomUUID(), first.id(), secondCourse.id(), EnrollmentStatus.WAITLISTED,
				now.plusSeconds(3), null, null, now.plusSeconds(3), now.plusSeconds(3), 0));
		enrollments.create(new Enrollment(UUID.randomUUID(), second.id(), firstCourse.id(), EnrollmentStatus.WAITLISTED,
				now.plusSeconds(4), null, null, now.plusSeconds(4), now.plusSeconds(4), 0));

		var firstPage = relationships.findCoursesByStudent(first.id(), new CursorRequest(1, null));
		List<Course> relatedCourses = new ArrayList<>(firstPage.content()); String cursor = firstPage.nextCursor();
		while (cursor != null) {
			var page = relationships.findCoursesByStudent(first.id(), new CursorRequest(1, cursor));
			relatedCourses.addAll(page.content()); cursor = page.nextCursor();
		}
		assertTrue(firstPage.hasNext());
		assertEquals(Set.of(firstCourse.id(), secondCourse.id()), relatedCourses.stream().map(Course::id).collect(Collectors.toSet()));
		assertEquals(2, relatedCourses.size());
		assertEquals(Set.of(first.id(), second.id()), relationships.findStudentsByCourse(firstCourse.id(),
				new CursorRequest(10, null)).content().stream().map(Student::id).collect(Collectors.toSet()));
		assertThrows(InvalidRequestException.class, () -> relationships.findCoursesByStudent(second.id(),
				new CursorRequest(1, firstPage.nextCursor())));
	}

	private static AtomicInteger race(ThrowingAction first, ThrowingAction second) throws Exception {
		CountDownLatch start = new CountDownLatch(1); AtomicInteger successes = new AtomicInteger();
		try (var executor = Executors.newFixedThreadPool(2)) {
			var tasks = List.of(first, second).stream().map(action -> executor.submit(() -> {
				start.await();
				try { action.run(); successes.incrementAndGet(); } catch (ConflictException ignored) { }
				return null;
			})).toList();
			start.countDown(); for (var task : tasks) task.get();
		}
		return successes;
	}

	private static void assertDepartmentCounts(UUID id, long students, long instructors, long courses) {
		DepartmentDynamoRecord record = tables.departments().getItem(request -> request.key(key(id)).consistentRead(true));
		assertEquals(students, record.getStudentCount()); assertEquals(instructors, record.getInstructorCount());
		assertEquals(courses, record.getCourseCount());
	}

	private static void assertInstructorCourseCount(UUID id, long expected) {
		assertEquals(expected, tables.instructors().getItem(request -> request.key(key(id)).consistentRead(true)).getCourseCount());
	}

	@FunctionalInterface
	private interface ThrowingAction { void run() throws Exception; }

	private static Student createStudent(String number, UUID departmentId, Instant now) {
		return students.create(new Student(UUID.randomUUID(), number, "Capacity", "Student",
				number.toLowerCase(java.util.Locale.ROOT) + "@example.com", StudentStatus.ACTIVE, departmentId, now, now, 0));
	}

	private static long occupiedSeats(UUID courseId) {
		return tables.courses().getItem(request -> request.key(key(courseId)).consistentRead(true)).getOccupiedSeats();
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

	private static UUID seedId(String name) {
		return UUID.nameUUIDFromBytes(("student-portal:" + name).getBytes(StandardCharsets.UTF_8));
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
					new IndexSpec("enrollments-catalog", "entityType", "enrolledAtId"),
					new IndexSpec("enrollment-relationships-by-student", "relationshipStudentId", "relationshipCourseId"),
					new IndexSpec("enrollment-relationships-by-course", "relationshipCourseId", "relationshipStudentId")));
}
