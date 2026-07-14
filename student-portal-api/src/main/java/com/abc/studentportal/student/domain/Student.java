package com.abc.studentportal.student.domain;

import com.abc.studentportal.common.domain.DomainChecks;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public record Student(UUID id, String studentNumber, String firstName, String lastName, String email,
		StudentStatus status, UUID departmentId, Instant createdAt, Instant updatedAt, long version) {

	public Student {
		DomainChecks.audit(id, createdAt, updatedAt, version);
		studentNumber = DomainChecks.requiredText(studentNumber, "studentNumber");
		firstName = DomainChecks.requiredText(firstName, "firstName");
		lastName = DomainChecks.requiredText(lastName, "lastName");
		email = DomainChecks.requiredText(email, "email").toLowerCase(Locale.ROOT);
		DomainChecks.required(status, "status");
		DomainChecks.required(departmentId, "departmentId");
	}

	public boolean mayEnroll() {
		return status == StudentStatus.ACTIVE;
	}

	public Student changeStatus(StudentStatus target, Instant changedAt) {
		DomainChecks.required(target, "status");
		DomainChecks.required(changedAt, "changedAt");
		return new Student(id, studentNumber, firstName, lastName, email, target, departmentId,
				createdAt, changedAt, version);
	}
}
