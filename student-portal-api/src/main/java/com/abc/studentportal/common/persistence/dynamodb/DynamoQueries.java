package com.abc.studentportal.common.persistence.dynamodb;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.Optional;

public final class DynamoQueries {

    private DynamoQueries() {
    }

    public static <T> boolean exists(DynamoDbIndex<T> index, String partitionValue) {
        return findOne(index, partitionValue).isPresent();
    }

    public static <T> Optional<T> findOne(DynamoDbIndex<T> index, String partitionValue) {
        return index.query(request -> request.queryConditional(QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(partitionValue).build())).limit(1))
                .stream().flatMap(page -> page.items().stream()).findFirst();
    }

}
