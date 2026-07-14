package com.abc.studentportal.student.persistence.dynamodb;

import com.abc.studentportal.common.persistence.dynamodb.DynamoSortKeys;
import com.abc.studentportal.student.domain.Student;
import com.abc.studentportal.student.domain.StudentStatus;

import java.time.Instant;
import java.util.UUID;

public final class StudentDynamoMapper {
	private StudentDynamoMapper() {
	}

	public static StudentDynamoRecord toRecord(Student value) {
		StudentDynamoRecord record = new StudentDynamoRecord();
		record.setId(value.id().toString());
		record.setStudentNumber(value.studentNumber());
		record.setFirstName(value.firstName());
		record.setLastName(value.lastName());
		record.setEmail(value.email());
		record.setStatus(value.status().name());
		record.setDepartmentId(value.departmentId().toString());
		record.setEntityType("STUDENT");
		record.setCreatedAt(value.createdAt().toString());
		record.setUpdatedAt(value.updatedAt().toString());
		record.setCreatedAtId(DynamoSortKeys.timestampId(value.createdAt(), value.id()));
		record.setUpdatedAtId(DynamoSortKeys.timestampId(value.updatedAt(), value.id()));
		record.setLastNameId(DynamoSortKeys.textId(value.lastName(), value.id()));
		record.setEnrollmentCount(0L);
		record.setVersion(value.version());
		return record;
	}

	public static Student toDomain(StudentDynamoRecord value) {
		return new Student(UUID.fromString(value.getId()), value.getStudentNumber(), value.getFirstName(),
				value.getLastName(),
				value.getEmail(), StudentStatus.valueOf(value.getStatus()), UUID.fromString(value.getDepartmentId()),
				Instant.parse(value.getCreatedAt()), Instant.parse(value.getUpdatedAt()), value.getVersion());
	}
}
