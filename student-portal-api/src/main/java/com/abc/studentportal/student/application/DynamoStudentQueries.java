package com.abc.studentportal.student.application;

import com.abc.studentportal.common.pagination.CursorPage;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.student.domain.Student;
import com.abc.studentportal.student.domain.StudentStatus;

import java.util.UUID;

public interface DynamoStudentQueries {
	CursorPage<Student> findAll(CursorRequest request);

	CursorPage<Student> findByDepartment(UUID departmentId, String lastNamePrefix, CursorRequest request);

	CursorPage<Student> findByStatus(StudentStatus status, CursorRequest request);
}
