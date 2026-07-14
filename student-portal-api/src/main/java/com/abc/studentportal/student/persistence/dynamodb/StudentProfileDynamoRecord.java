package com.abc.studentportal.student.persistence.dynamodb;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
@Getter
@Setter
@NoArgsConstructor
public class StudentProfileDynamoRecord implements com.abc.studentportal.common.persistence.dynamodb.VersionedDynamoRecord {
	private String studentId;
	private String id;
	private String dateOfBirth;
	private String phoneNumber;
	private String addressLine1;
	private String addressLine2;
	private String city;
	private String state;
	private String postalCode;
	private String country;
	private String createdAt;
	private String updatedAt;
	private Long version;

	@DynamoDbPartitionKey public String getStudentId() { return studentId; }
	@DynamoDbVersionAttribute public Long getVersion() { return version; }
}
