package com.abc.studentportal.student.domain;

import com.abc.studentportal.common.domain.DomainRuleViolationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StudentTest {

	private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

	@Test
	void normalizesEmailAndDeterminesEnrollmentEligibility() {
		Student student = student(StudentStatus.ACTIVE, " Student@Example.COM ");

		assertThat(student.email()).isEqualTo("student@example.com");
		assertThat(student.mayEnroll()).isTrue();
		assertThat(student.changeStatus(StudentStatus.SUSPENDED, NOW.plusSeconds(1)).mayEnroll()).isFalse();
	}

	@Test
	void rejectsMissingRequiredFieldsAndInvalidAuditOrder() {
		assertThatThrownBy(() -> student(StudentStatus.ACTIVE, " "))
				.isInstanceOf(DomainRuleViolationException.class)
				.hasMessage("email is required");

		assertThatThrownBy(() -> new Student(UUID.randomUUID(), "S1", "A", "B", "a@b.test",
				StudentStatus.ACTIVE, UUID.randomUUID(), NOW, NOW.minusSeconds(1), 0))
				.isInstanceOf(DomainRuleViolationException.class)
				.hasMessage("updatedAt must not be before createdAt");
	}

	private static Student student(StudentStatus status, String email) {
		return new Student(UUID.randomUUID(), "S1", "Ada", "Lovelace", email, status,
				UUID.randomUUID(), NOW, NOW, 0);
	}
}
