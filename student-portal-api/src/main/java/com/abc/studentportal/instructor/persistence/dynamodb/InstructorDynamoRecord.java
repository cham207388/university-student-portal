package com.abc.studentportal.instructor.persistence.dynamodb;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

@DynamoDbBean
@Getter
@Setter
@NoArgsConstructor
public class InstructorDynamoRecord implements com.abc.studentportal.common.persistence.dynamodb.VersionedDynamoRecord {
	private String id;
	private String employeeNumber;
	private String firstName;
	private String lastName;
	private String email;
	private String departmentId;
	private String entityType;
	private String createdAt;
	private String updatedAt;
	private String createdAtId;
	private String lastNameId;
	private Long courseCount;
	private Long version;

	@DynamoDbPartitionKey public String getId() { return id; }
	@DynamoDbSecondaryPartitionKey(indexNames = "instructors-by-number") public String getEmployeeNumber() { return employeeNumber; }
	@DynamoDbSecondaryPartitionKey(indexNames = "instructors-by-email") public String getEmail() { return email; }
	@DynamoDbSecondaryPartitionKey(indexNames = "instructors-by-department") public String getDepartmentId() { return departmentId; }
	@DynamoDbSecondaryPartitionKey(indexNames = "instructors-catalog") public String getEntityType() { return entityType; }
	@DynamoDbSecondarySortKey(indexNames = "instructors-catalog") public String getCreatedAtId() { return createdAtId; }
	@DynamoDbSecondarySortKey(indexNames = "instructors-by-department") public String getLastNameId() { return lastNameId; }
	@DynamoDbVersionAttribute public Long getVersion() { return version; }
}
