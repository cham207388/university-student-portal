package com.abc.studentportal.enrollment.domain;

import com.abc.studentportal.common.domain.DomainRuleViolationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnrollmentTest {

	private static final Instant ENROLLED_AT = Instant.parse("2026-01-01T00:00:00Z");

	@Test
	void dropsAnActiveEnrollmentAndRecordsDropTime() {
		Enrollment enrollment = enrollment(EnrollmentStatus.ENROLLED, null, null);
		Instant droppedAt = ENROLLED_AT.plusSeconds(60);

		Enrollment dropped = enrollment.transitionTo(EnrollmentStatus.DROPPED, null, droppedAt);

		assertThat(dropped.status()).isEqualTo(EnrollmentStatus.DROPPED);
		assertThat(dropped.droppedAt()).isEqualTo(droppedAt);
		assertThat(dropped.isActive()).isFalse();
		assertThat(dropped.consumesCapacity()).isFalse();
	}

	@Test
	void allowsGradeOnlyWhenCompleted() {
		assertThatThrownBy(() -> enrollment(EnrollmentStatus.ENROLLED, null, "A"))
				.isInstanceOf(DomainRuleViolationException.class)
				.hasMessage("finalGrade is only valid for a completed enrollment");

		Enrollment completed = enrollment(EnrollmentStatus.COMPLETED, null, "A");
		assertThat(completed.finalGrade()).isEqualTo("A");
		assertThat(completed.consumesCapacity()).isTrue();
	}

	@Test
	void rejectsMissingDropTimestampAndTerminalTransitions() {
		assertThatThrownBy(() -> enrollment(EnrollmentStatus.DROPPED, null, null))
				.isInstanceOf(DomainRuleViolationException.class)
				.hasMessage("droppedAt is required for a dropped enrollment");

		Enrollment completed = enrollment(EnrollmentStatus.COMPLETED, null, null);
		assertThatThrownBy(() -> completed.transitionTo(EnrollmentStatus.DROPPED, null, ENROLLED_AT.plusSeconds(60)))
				.isInstanceOf(DomainRuleViolationException.class)
				.hasMessage("enrollment cannot transition from COMPLETED to DROPPED");
	}

	private static Enrollment enrollment(EnrollmentStatus status, Instant droppedAt, String grade) {
		return new Enrollment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), status, ENROLLED_AT,
				droppedAt, grade, ENROLLED_AT, ENROLLED_AT, 0);
	}
}
