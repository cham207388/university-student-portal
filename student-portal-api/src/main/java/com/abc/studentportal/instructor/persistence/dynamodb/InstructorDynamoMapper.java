package com.abc.studentportal.instructor.persistence.dynamodb;

import com.abc.studentportal.common.persistence.dynamodb.DynamoSortKeys;
import com.abc.studentportal.instructor.domain.Instructor;

import java.time.Instant;
import java.util.UUID;

public final class InstructorDynamoMapper {
	private InstructorDynamoMapper() { }

	public static InstructorDynamoRecord toRecord(Instructor value) {
		InstructorDynamoRecord record = new InstructorDynamoRecord();
		record.setId(value.id().toString()); record.setEmployeeNumber(value.employeeNumber());
		record.setFirstName(value.firstName()); record.setLastName(value.lastName()); record.setEmail(value.email());
		record.setDepartmentId(value.departmentId().toString()); record.setEntityType("INSTRUCTOR");
		record.setCreatedAt(value.createdAt().toString()); record.setUpdatedAt(value.updatedAt().toString());
		record.setCreatedAtId(DynamoSortKeys.timestampId(value.createdAt(), value.id()));
		record.setLastNameId(DynamoSortKeys.textId(value.lastName(), value.id())); record.setVersion(value.version()); return record;
	}

	public static Instructor toDomain(InstructorDynamoRecord value) {
		return new Instructor(UUID.fromString(value.getId()), value.getEmployeeNumber(), value.getFirstName(), value.getLastName(),
				value.getEmail(), UUID.fromString(value.getDepartmentId()), Instant.parse(value.getCreatedAt()),
				Instant.parse(value.getUpdatedAt()), value.getVersion());
	}
}
