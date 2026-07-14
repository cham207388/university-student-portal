package com.abc.studentportal.enrollment.persistence.dynamodb;

import com.abc.studentportal.common.persistence.dynamodb.DynamoSortKeys;
import com.abc.studentportal.enrollment.domain.Enrollment;
import com.abc.studentportal.enrollment.domain.EnrollmentStatus;

import java.time.Instant;
import java.util.UUID;

public final class EnrollmentDynamoMapper {
	private EnrollmentDynamoMapper() {
	}

	public static EnrollmentDynamoRecord toRecord(Enrollment value) {
		EnrollmentDynamoRecord record = new EnrollmentDynamoRecord();
		record.setId(value.id().toString());
		record.setRecordType("ENROLLMENT");
		record.setStudentId(value.studentId().toString());
		record.setCourseId(value.courseId().toString());
		record.setStatus(value.status().name());
		record.setEnrolledAt(value.enrolledAt().toString());
		record.setDroppedAt(value.droppedAt() == null ? null : value.droppedAt().toString());
		record.setFinalGrade(value.finalGrade());
		record.setCreatedAt(value.createdAt().toString());
		record.setUpdatedAt(value.updatedAt().toString());
		record.setEntityType("ENROLLMENT");
		record.setEnrolledAtId(DynamoSortKeys.timestampId(value.enrolledAt(), value.id()));
		record.setVersion(value.version());
		return record;
	}

	public static Enrollment toDomain(EnrollmentDynamoRecord value) {
		return new Enrollment(UUID.fromString(value.getId()), UUID.fromString(value.getStudentId()),
				UUID.fromString(value.getCourseId()), EnrollmentStatus.valueOf(value.getStatus()),
				Instant.parse(value.getEnrolledAt()),
				value.getDroppedAt() == null ? null : Instant.parse(value.getDroppedAt()),
				value.getFinalGrade(), Instant.parse(value.getCreatedAt()), Instant.parse(value.getUpdatedAt()),
				value.getVersion());
	}
}
