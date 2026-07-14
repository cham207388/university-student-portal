package com.abc.studentportal.student.application;

import com.abc.studentportal.student.domain.StudentStatus;

import java.util.UUID;

public record StudentFilter(UUID departmentId, StudentStatus status, String email,
		String studentNumber, String lastName) {
}
