package com.abc.studentportal.common.persistence.dynamodb;

import com.abc.studentportal.common.exception.ConflictException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public abstract class AbstractDynamoRepository<D, R extends VersionedDynamoRecord> {
	private final DynamoDbTable<R> table;
	private final String partitionKeyName;
	private final Function<D, R> toRecord;
	private final Function<R, D> toDomain;
	private final Function<D, String> domainKey;

	protected AbstractDynamoRepository(DynamoDbTable<R> table, String partitionKeyName,
			Function<D, R> toRecord, Function<R, D> toDomain, Function<D, String> domainKey) {
		this.table = table;
		this.partitionKeyName = partitionKeyName;
		this.toRecord = toRecord;
		this.toDomain = toDomain;
		this.domainKey = domainKey;
	}

	protected D createItem(D value) {
		R record = toRecord.apply(value);
		record.setVersion(null);
		try {
			table.putItem(request -> request.item(record).conditionExpression(attributeDoesNotExist()));
			return findItem(domainKey.apply(value)).orElseThrow();
		} catch (ConditionalCheckFailedException exception) {
			throw new ConflictException("Resource already exists");
		}
	}

	protected D updateItem(D value) {
		return updateRecord(toRecord.apply(value));
	}

	protected D updateRecord(R record) {
		try {
			return toDomain.apply(table.updateItem(record));
		} catch (ConditionalCheckFailedException exception) {
			throw new ConflictException("Resource was modified by another request");
		}
	}

	protected Optional<D> findItem(String key) {
		R record = table.getItem(request -> request.key(key(key)).consistentRead(true));
		return Optional.ofNullable(record).map(toDomain);
	}

	protected void deleteItem(D value, long version) {
		Expression expectedVersion = Expression.builder()
				.expression("#version = :version")
				.expressionNames(Map.of("#version", "version"))
				.expressionValues(Map.of(":version", AttributeValue.builder().n(Long.toString(version)).build()))
				.build();
		try {
			table.deleteItem(request -> request.key(key(domainKey.apply(value))).conditionExpression(expectedVersion));
		} catch (ConditionalCheckFailedException exception) {
			throw new ConflictException("Resource was modified by another request");
		}
	}

	protected DynamoDbTable<R> table() {
		return table;
	}

	private Expression attributeDoesNotExist() {
		return Expression.builder().expression("attribute_not_exists(#pk)")
				.expressionNames(Map.of("#pk", partitionKeyName)).build();
	}

	protected static Key key(String value) {
		return Key.builder().partitionValue(value).build();
	}
}
