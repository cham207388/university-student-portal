package com.abc.studentportal.common.persistence.dynamodb;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.Update;

import java.util.Map;

public final class DynamoRelationshipCounters {

    private final DynamoDbTables tables;

    public DynamoRelationshipCounters(DynamoDbTables tables) {
        this.tables = tables;
    }

    public TransactWriteItem department(String departmentId, String counter, int delta) {
        return update(tables.departments().tableName(), departmentId, counter, delta);
    }

    public TransactWriteItem instructor(String instructorId, int delta) {
        return update(tables.instructors().tableName(), instructorId, "courseCount", delta);
    }

    private static TransactWriteItem update(String table, String id, String counter, int delta) {
        String condition = delta > 0 ? "attribute_exists(id)" : "attribute_exists(id) AND #counter > :zero";
        return TransactWriteItem.builder().update(Update.builder().tableName(table)
                .key(Map.of("id", string(id)))
                .updateExpression("SET #counter = if_not_exists(#counter, :zero) + :delta, #version = #version + :one")
                .conditionExpression(condition)
                .expressionAttributeNames(Map.of("#counter", counter, "#version", "version"))
                .expressionAttributeValues(Map.of(":zero", number(0), ":delta", number(delta), ":one", number(1)))
                .build()).build();
    }

    private static AttributeValue string(String value) {
        return AttributeValue.builder().s(value).build();
    }

    private static AttributeValue number(long value) {
        return AttributeValue.builder().n(Long.toString(value)).build();
    }

}
