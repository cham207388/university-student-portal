package com.abc.studentportal.enrollment.persistence.dynamodb;

import com.abc.studentportal.common.exception.ConflictException;
import com.abc.studentportal.common.persistence.dynamodb.DynamoDbTables;
import com.abc.studentportal.enrollment.domain.Enrollment;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.Update;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class DynamoEnrollmentTransactionWriter {
	private final DynamoDbClient client;
	private final DynamoDbTables tables;

	public DynamoEnrollmentTransactionWriter(DynamoDbClient client, DynamoDbTables tables) {
		this.client = client;
		this.tables = tables;
	}

	public Enrollment create(Enrollment value) {
		EnrollmentDynamoRecord record = EnrollmentDynamoMapper.toRecord(value);
		record.setVersion(1L);
		List<TransactWriteItem> actions = new ArrayList<>();
		actions.add(addStudentEnrollment(value));
		actions.add(addCourseEnrollment(value.courseId().toString(), value.consumesCapacity()));
		actions.add(putEnrollment(record, "attribute_not_exists(id)", Map.of()));
		actions.add(putRelationship(value));
		if (value.isActive())
			actions.add(putActiveLock(value));
		execute(actions, "Enrollment could not be created because a reference, capacity, or active enrollment changed");
		return EnrollmentDynamoMapper.toDomain(record);
	}

	public Enrollment update(Enrollment previous, Enrollment next) {
		EnrollmentDynamoRecord record = EnrollmentDynamoMapper.toRecord(next);
		record.setVersion(next.version() + 1);
		List<TransactWriteItem> actions = new ArrayList<>();
		int capacityDelta = Boolean.compare(next.consumesCapacity(), previous.consumesCapacity());
		if (capacityDelta != 0)
			actions.add(changeCapacity(next.courseId().toString(), capacityDelta));
		actions.add(putEnrollment(record, "#version = :version", Map.of(":version", number(next.version()))));
		if (previous.isActive() && !next.isActive())
			actions.add(deleteActiveLock(previous));
		else if (!previous.isActive() && next.isActive())
			actions.add(putActiveLock(next));
		execute(actions, "Enrollment was modified or its course capacity/active lock changed");
		return EnrollmentDynamoMapper.toDomain(record);
	}

	public void ensureRelationship(Enrollment value) {
		client.putItem(request -> request.tableName(tables.enrollments().tableName())
				.item(putRelationship(value).put().item()));
	}

	private TransactWriteItem addStudentEnrollment(Enrollment value) {
		return TransactWriteItem.builder().update(Update.builder()
				.tableName(tables.students().tableName()).key(key(value.studentId().toString()))
				.updateExpression(
						"SET enrollmentCount = if_not_exists(enrollmentCount, :zero) + :one, #version = #version + :one")
				.conditionExpression("attribute_exists(id) AND #status = :active")
				.expressionAttributeNames(Map.of("#status", "status", "#version", "version"))
				.expressionAttributeValues(Map.of(":active", string("ACTIVE"), ":zero", number(0), ":one", number(1)))
				.build()).build();
	}

	private TransactWriteItem addCourseEnrollment(String courseId, boolean consumesCapacity) {
		String update = consumesCapacity
				? "SET enrollmentCount = if_not_exists(enrollmentCount, :zero) + :one, #occupied = #occupied + :one, #version = #version + :one"
				: "SET enrollmentCount = if_not_exists(enrollmentCount, :zero) + :one, #version = #version + :one";
		String condition = consumesCapacity
				? "attribute_exists(id) AND #status = :open AND #occupied < #capacity"
				: "attribute_exists(id) AND #status = :open";
		Map<String, String> names = consumesCapacity
				? Map.of("#status", "status", "#occupied", "occupiedSeats", "#capacity", "capacity", "#version",
						"version")
				: Map.of("#status", "status", "#version", "version");
		return TransactWriteItem.builder()
				.update(Update.builder().tableName(tables.courses().tableName()).key(key(courseId))
						.updateExpression(update).conditionExpression(condition).expressionAttributeNames(names)
						.expressionAttributeValues(
								Map.of(":open", string("OPEN"), ":zero", number(0), ":one", number(1)))
						.build())
				.build();
	}

	private TransactWriteItem changeCapacity(String courseId, int delta) {
		String condition = delta > 0
				? "attribute_exists(id) AND #status = :open AND #occupied < #capacity"
				: "attribute_exists(id) AND #occupied > :zero";
		Map<String, AttributeValue> values = delta > 0
				? Map.of(":delta", number(delta), ":one", number(1), ":open", string("OPEN"))
				: Map.of(":delta", number(delta), ":one", number(1), ":zero", number(0));
		Map<String, String> names = delta > 0
				? Map.of("#status", "status", "#version", "version", "#occupied", "occupiedSeats", "#capacity",
						"capacity")
				: Map.of("#version", "version", "#occupied", "occupiedSeats");
		return TransactWriteItem.builder().update(Update.builder().tableName(tables.courses().tableName())
				.key(key(courseId)).updateExpression("SET #occupied = #occupied + :delta, #version = #version + :one")
				.conditionExpression(condition).expressionAttributeNames(names)
				.expressionAttributeValues(values).build()).build();
	}

	private TransactWriteItem putEnrollment(EnrollmentDynamoRecord record, String condition,
			Map<String, AttributeValue> values) {
		Put.Builder put = Put.builder().tableName(tables.enrollments().tableName())
				.item(tables.enrollments().tableSchema().itemToMap(record, true)).conditionExpression(condition);
		if (condition.contains("#version"))
			put.expressionAttributeNames(Map.of("#version", "version"));
		if (!values.isEmpty())
			put.expressionAttributeValues(values);
		return TransactWriteItem.builder().put(put.build()).build();
	}

	private TransactWriteItem putActiveLock(Enrollment value) {
		Map<String, AttributeValue> item = Map.of("id", string(lockId(value)), "recordType",
				string("ACTIVE_ENROLLMENT_LOCK"), "ownerId", string(value.id().toString()));
		return TransactWriteItem.builder().put(Put.builder().tableName(tables.enrollments().tableName()).item(item)
				.conditionExpression("attribute_not_exists(id)").build()).build();
	}

	private TransactWriteItem putRelationship(Enrollment value) {
		Map<String, AttributeValue> item = Map.of(
				"id", string("RELATIONSHIP#" + value.studentId() + "#" + value.courseId()),
				"recordType", string("STUDENT_COURSE_RELATIONSHIP"),
				"relationshipStudentId", string(value.studentId().toString()),
				"relationshipCourseId", string(value.courseId().toString()));
		return TransactWriteItem.builder()
				.put(Put.builder().tableName(tables.enrollments().tableName()).item(item).build()).build();
	}

	private TransactWriteItem deleteActiveLock(Enrollment value) {
		return TransactWriteItem.builder().delete(Delete.builder().tableName(tables.enrollments().tableName())
				.key(key(lockId(value))).conditionExpression("ownerId = :owner")
				.expressionAttributeValues(Map.of(":owner", string(value.id().toString()))).build()).build();
	}

	private void execute(List<TransactWriteItem> actions, String message) {
		try {
			client.transactWriteItems(request -> request.transactItems(actions));
		} catch (TransactionCanceledException exception) {
			throw new ConflictException(message);
		}
	}

	private static String lockId(Enrollment value) {
		return "ACTIVE#" + value.studentId() + "#" + value.courseId();
	}

	private static Map<String, AttributeValue> key(String id) {
		return Map.of("id", string(id));
	}

	private static AttributeValue string(String value) {
		return AttributeValue.builder().s(value).build();
	}

	private static AttributeValue number(long value) {
		return AttributeValue.builder().n(Long.toString(value)).build();
	}
}
