package com.abc.studentportal.common.persistence.dynamodb;

import com.abc.studentportal.course.persistence.dynamodb.CourseDynamoRecord;
import com.abc.studentportal.department.persistence.dynamodb.DepartmentDynamoRecord;
import com.abc.studentportal.enrollment.persistence.dynamodb.EnrollmentDynamoRecord;
import com.abc.studentportal.instructor.persistence.dynamodb.InstructorDynamoRecord;
import com.abc.studentportal.student.persistence.dynamodb.StudentDynamoRecord;
import com.abc.studentportal.student.persistence.dynamodb.StudentProfileDynamoRecord;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

public record DynamoDbTables(
		DynamoDbTable<DepartmentDynamoRecord> departments,
		DynamoDbTable<StudentDynamoRecord> students,
		DynamoDbTable<StudentProfileDynamoRecord> studentProfiles,
		DynamoDbTable<InstructorDynamoRecord> instructors,
		DynamoDbTable<CourseDynamoRecord> courses,
		DynamoDbTable<EnrollmentDynamoRecord> enrollments) {
}
