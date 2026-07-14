package com.abc.studentportal.department.persistence.dynamodb;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;

@DynamoDbBean
@Getter
@Setter
@NoArgsConstructor
public class DepartmentDynamoRecord implements com.abc.studentportal.common.persistence.dynamodb.VersionedDynamoRecord {
	private String id;
	private String code;
	private String name;
	private String description;
	private String entityType;
	private String createdAt;
	private String updatedAt;
	private String createdAtId;
	private Long studentCount;
	private Long instructorCount;
	private Long courseCount;
	private Long version;

	@DynamoDbPartitionKey
	public String getId() { return id; }

	@DynamoDbSecondaryPartitionKey(indexNames = "departments-by-code")
	public String getCode() { return code; }

	@DynamoDbSecondaryPartitionKey(indexNames = "departments-catalog")
	public String getEntityType() { return entityType; }

	@DynamoDbSecondarySortKey(indexNames = "departments-catalog")
	public String getCreatedAtId() { return createdAtId; }

	@DynamoDbVersionAttribute
	public Long getVersion() { return version; }
}
