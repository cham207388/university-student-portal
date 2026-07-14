package com.abc.studentportal.enrollment.persistence.dynamodb;

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
public class EnrollmentDynamoRecord implements com.abc.studentportal.common.persistence.dynamodb.VersionedDynamoRecord {
	private String id;
	private String recordType;
	private String studentId;
	private String courseId;
	private String status;
	private String enrolledAt;
	private String droppedAt;
	private String finalGrade;
	private String createdAt;
	private String updatedAt;
	private String entityType;
	private String enrolledAtId;
	private Long version;

	@DynamoDbPartitionKey public String getId() { return id; }
	@DynamoDbSecondaryPartitionKey(indexNames = "enrollments-by-student") public String getStudentId() { return studentId; }
	@DynamoDbSecondaryPartitionKey(indexNames = "enrollments-by-course") public String getCourseId() { return courseId; }
	@DynamoDbSecondaryPartitionKey(indexNames = "enrollments-by-status") public String getStatus() { return status; }
	@DynamoDbSecondaryPartitionKey(indexNames = "enrollments-catalog") public String getEntityType() { return entityType; }
	@DynamoDbSecondarySortKey(indexNames = {"enrollments-by-student", "enrollments-by-course", "enrollments-by-status", "enrollments-catalog"})
	public String getEnrolledAtId() { return enrolledAtId; }
	@DynamoDbVersionAttribute public Long getVersion() { return version; }
}
