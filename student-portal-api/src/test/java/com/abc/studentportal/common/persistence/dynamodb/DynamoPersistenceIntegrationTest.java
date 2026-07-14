package com.abc.studentportal.common.persistence.dynamodb;

import com.abc.studentportal.common.exception.ConflictException;
import com.abc.studentportal.course.persistence.dynamodb.CourseDynamoRecord;
import com.abc.studentportal.department.domain.Department;
import com.abc.studentportal.department.persistence.dynamodb.DepartmentDynamoRecord;
import com.abc.studentportal.department.persistence.dynamodb.DynamoDepartmentRepository;
import com.abc.studentportal.enrollment.persistence.dynamodb.EnrollmentDynamoRecord;
import com.abc.studentportal.instructor.persistence.dynamodb.InstructorDynamoRecord;
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
	private static DynamoDepartmentRepository departments;

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
		DynamoDbTables tables = new DynamoDbTables(
				enhanced.table(name("departments"), TableSchema.fromBean(DepartmentDynamoRecord.class)),
				enhanced.table(name("students"), TableSchema.fromBean(StudentDynamoRecord.class)),
				enhanced.table(name("student-profiles"), TableSchema.fromBean(StudentProfileDynamoRecord.class)),
				enhanced.table(name("instructors"), TableSchema.fromBean(InstructorDynamoRecord.class)),
				enhanced.table(name("courses"), TableSchema.fromBean(CourseDynamoRecord.class)),
				enhanced.table(name("enrollments"), TableSchema.fromBean(EnrollmentDynamoRecord.class)));
		departments = new DynamoDepartmentRepository(tables);
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

		departments.delete(updated);
		assertFalse(departments.findById(id).isPresent());
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
