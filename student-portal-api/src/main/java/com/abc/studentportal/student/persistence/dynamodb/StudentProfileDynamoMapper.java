package com.abc.studentportal.student.persistence.dynamodb;

import com.abc.studentportal.student.domain.StudentProfile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class StudentProfileDynamoMapper {
	private StudentProfileDynamoMapper() { }

	public static StudentProfileDynamoRecord toRecord(StudentProfile value) {
		StudentProfileDynamoRecord record = new StudentProfileDynamoRecord();
		record.setStudentId(value.studentId().toString()); record.setId(value.id().toString());
		record.setDateOfBirth(value.dateOfBirth().toString()); record.setPhoneNumber(value.phoneNumber());
		record.setAddressLine1(value.addressLine1()); record.setAddressLine2(value.addressLine2());
		record.setCity(value.city()); record.setState(value.state()); record.setPostalCode(value.postalCode());
		record.setCountry(value.country()); record.setCreatedAt(value.createdAt().toString());
		record.setUpdatedAt(value.updatedAt().toString()); record.setVersion(value.version()); return record;
	}

	public static StudentProfile toDomain(StudentProfileDynamoRecord value) {
		return new StudentProfile(UUID.fromString(value.getId()), UUID.fromString(value.getStudentId()),
				LocalDate.parse(value.getDateOfBirth()), value.getPhoneNumber(), value.getAddressLine1(), value.getAddressLine2(),
				value.getCity(), value.getState(), value.getPostalCode(), value.getCountry(), Instant.parse(value.getCreatedAt()),
				Instant.parse(value.getUpdatedAt()), value.getVersion());
	}
}
