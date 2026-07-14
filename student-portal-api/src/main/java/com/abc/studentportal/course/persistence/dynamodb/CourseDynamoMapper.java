package com.abc.studentportal.course.persistence.dynamodb;

import com.abc.studentportal.common.persistence.dynamodb.DynamoSortKeys;
import com.abc.studentportal.course.domain.Course;
import com.abc.studentportal.course.domain.CourseStatus;

import java.time.Instant;
import java.util.UUID;

public final class CourseDynamoMapper {
	private CourseDynamoMapper() { }

	public static CourseDynamoRecord toRecord(Course value) {
		CourseDynamoRecord record = new CourseDynamoRecord();
		record.setId(value.id().toString()); record.setCourseCode(value.courseCode()); record.setTitle(value.title());
		record.setDescription(value.description()); record.setCredits(value.credits()); record.setCapacity(value.capacity());
		record.setOccupiedSeats(0L); record.setEnrollmentCount(0L); record.setStatus(value.status().name());
		record.setDepartmentId(value.departmentId().toString()); record.setInstructorId(value.instructorId().toString());
		record.setEntityType("COURSE"); record.setCreatedAt(value.createdAt().toString());
		record.setUpdatedAt(value.updatedAt().toString());
		record.setCreatedAtId(DynamoSortKeys.timestampId(value.createdAt(), value.id()));
		record.setUpdatedAtId(DynamoSortKeys.timestampId(value.updatedAt(), value.id()));
		record.setCourseCodeId(DynamoSortKeys.textId(value.courseCode(), value.id())); record.setVersion(value.version()); return record;
	}

	public static Course toDomain(CourseDynamoRecord value) {
		return new Course(UUID.fromString(value.getId()), value.getCourseCode(), value.getTitle(), value.getDescription(),
				value.getCredits(), value.getCapacity(), CourseStatus.valueOf(value.getStatus()),
				UUID.fromString(value.getDepartmentId()), UUID.fromString(value.getInstructorId()),
				Instant.parse(value.getCreatedAt()), Instant.parse(value.getUpdatedAt()), value.getVersion());
	}
}
