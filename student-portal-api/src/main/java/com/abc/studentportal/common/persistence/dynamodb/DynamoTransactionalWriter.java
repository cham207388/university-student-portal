package com.abc.studentportal.common.persistence.dynamodb;

import com.abc.studentportal.common.exception.ConflictException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class DynamoTransactionalWriter {
	private final DynamoDbClient client;

	public DynamoTransactionalWriter(DynamoDbClient client) {
		this.client = client;
	}

	public <R extends VersionedDynamoRecord> R create(DynamoDbTable<R> table, String partitionKey, R record,
			List<DynamoUniqueClaim> claims) {
		record.setVersion(1L);
		List<TransactWriteItem> actions = new ArrayList<>();
		actions.add(TransactWriteItem.builder().put(Put.builder().tableName(table.tableName())
				.item(table.tableSchema().itemToMap(record, true)).conditionExpression("attribute_not_exists(#pk)")
				.expressionAttributeNames(Map.of("#pk", partitionKey)).build()).build());
		claims.forEach(claim -> actions.add(putClaim(table.tableName(), partitionKey, claim)));
		execute(actions, "Resource or alternate key already exists");
		return record;
	}

	public <R extends VersionedDynamoRecord> R update(DynamoDbTable<R> table, String partitionKey, R record,
			long expectedVersion, List<DynamoUniqueClaim> previousClaims, List<DynamoUniqueClaim> nextClaims) {
		record.setVersion(expectedVersion + 1);
		List<TransactWriteItem> actions = new ArrayList<>();
		actions.add(TransactWriteItem.builder().put(Put.builder().tableName(table.tableName())
				.item(table.tableSchema().itemToMap(record, true)).conditionExpression("#version = :version")
				.expressionAttributeNames(Map.of("#version", "version"))
				.expressionAttributeValues(Map.of(":version", number(expectedVersion))).build()).build());
		for (DynamoUniqueClaim previous : previousClaims) {
			if (!nextClaims.contains(previous)) actions.add(deleteClaim(table.tableName(), partitionKey, previous));
		}
		for (DynamoUniqueClaim next : nextClaims) {
			if (!previousClaims.contains(next)) actions.add(putClaim(table.tableName(), partitionKey, next));
		}
		execute(actions, "Resource was modified or an alternate key is already in use");
		return record;
	}

	public void delete(String table, String partitionKey, String entityId, long expectedVersion,
			List<DynamoUniqueClaim> claims) {
		delete(table, partitionKey, entityId, expectedVersion, claims, List.of(), false);
	}

	public void delete(String table, String partitionKey, String entityId, long expectedVersion,
			List<DynamoUniqueClaim> claims, List<TransactWriteItem> additionalActions) {
		delete(table, partitionKey, entityId, expectedVersion, claims, additionalActions, false);
	}

	public void delete(String table, String partitionKey, String entityId, long expectedVersion,
			List<DynamoUniqueClaim> claims, List<TransactWriteItem> additionalActions, boolean requireNoEnrollments) {
		List<TransactWriteItem> actions = new ArrayList<>();
		String condition = requireNoEnrollments
				? "#version = :version AND (attribute_not_exists(enrollmentCount) OR enrollmentCount = :zero)"
				: "#version = :version";
		Map<String, AttributeValue> values = requireNoEnrollments
				? Map.of(":version", number(expectedVersion), ":zero", number(0))
				: Map.of(":version", number(expectedVersion));
		actions.add(TransactWriteItem.builder().delete(Delete.builder().tableName(table)
				.key(Map.of(partitionKey, string(entityId))).conditionExpression(condition)
				.expressionAttributeNames(Map.of("#version", "version"))
				.expressionAttributeValues(values).build()).build());
		claims.forEach(claim -> actions.add(deleteClaim(table, partitionKey, claim)));
		actions.addAll(additionalActions);
		execute(actions, "Resource was modified by another request");
	}

	private void execute(List<TransactWriteItem> actions, String conflictMessage) {
		try {
			client.transactWriteItems(request -> request.transactItems(actions));
		} catch (TransactionCanceledException exception) {
			throw new ConflictException(conflictMessage);
		}
	}

	private static TransactWriteItem putClaim(String table, String partitionKey, DynamoUniqueClaim claim) {
		Map<String, AttributeValue> item = Map.of(
				partitionKey, string(claim.id()),
				"recordType", string("UNIQUE_CLAIM"),
				"ownerId", string(claim.ownerId()));
		return TransactWriteItem.builder().put(Put.builder().tableName(table).item(item)
				.conditionExpression("attribute_not_exists(#pk)")
				.expressionAttributeNames(Map.of("#pk", partitionKey)).build()).build();
	}

	private static TransactWriteItem deleteClaim(String table, String partitionKey, DynamoUniqueClaim claim) {
		return TransactWriteItem.builder().delete(Delete.builder().tableName(table)
				.key(Map.of(partitionKey, string(claim.id())))
				.conditionExpression("#owner = :owner")
				.expressionAttributeNames(Map.of("#owner", "ownerId"))
				.expressionAttributeValues(Map.of(":owner", string(claim.ownerId()))).build()).build();
	}

	private static AttributeValue string(String value) { return AttributeValue.builder().s(value).build(); }
	private static AttributeValue number(long value) { return AttributeValue.builder().n(Long.toString(value)).build(); }
}
