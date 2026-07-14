package com.abc.studentportal.common.persistence.dynamodb;

import com.abc.studentportal.common.pagination.CursorPage;
import com.abc.studentportal.common.pagination.CursorRequest;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.function.Function;

public final class DynamoCursorQueries {
	private DynamoCursorQueries() { }

	public static <R, D> CursorPage<D> query(DynamoDbIndex<R> index, QueryConditional conditional,
			CursorRequest request, String queryIdentity, DynamoCursorCodec codec, Function<R, D> mapper) {
		var startKey = codec.decode(queryIdentity, request.cursor());
		var query = index.query(builder -> {
			builder.queryConditional(conditional).limit(request.limit());
			if (!startKey.isEmpty()) builder.exclusiveStartKey(startKey);
		});
		Page<R> page = query.iterator().next();
		String nextCursor = codec.encode(queryIdentity, page.lastEvaluatedKey());
		return new CursorPage<>(page.items().stream().map(mapper).toList(), request.limit(), nextCursor,
				nextCursor != null);
	}

	public static QueryConditional equalTo(String partitionValue) {
		return QueryConditional.keyEqualTo(Key.builder().partitionValue(partitionValue).build());
	}

	public static QueryConditional beginsWith(String partitionValue, String sortPrefix) {
		return QueryConditional.sortBeginsWith(Key.builder().partitionValue(partitionValue)
				.sortValue(sortPrefix).build());
	}

	public static QueryConditional between(String partitionValue, String from, String to) {
		Key lower = Key.builder().partitionValue(partitionValue).sortValue(from).build();
		Key upper = Key.builder().partitionValue(partitionValue).sortValue(to).build();
		return QueryConditional.sortBetween(lower, upper);
	}

	public static QueryConditional from(String partitionValue, String from) {
		return QueryConditional.sortGreaterThanOrEqualTo(Key.builder().partitionValue(partitionValue)
				.sortValue(from).build());
	}

	public static QueryConditional to(String partitionValue, String to) {
		return QueryConditional.sortLessThanOrEqualTo(Key.builder().partitionValue(partitionValue)
				.sortValue(to).build());
	}

	public static String identity(String table, String index, String... parameters) {
		StringBuilder identity = new StringBuilder();
		append(identity, table); append(identity, index);
		for (String parameter : parameters) append(identity, parameter);
		return identity.toString();
	}

	private static void append(StringBuilder target, String value) {
		if (value == null) target.append("-1:");
		else target.append(value.length()).append(':').append(value);
	}
}
