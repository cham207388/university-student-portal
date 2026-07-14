package com.abc.studentportal.course.domain;

import com.abc.studentportal.common.domain.DomainRuleViolationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseTest {

	private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

	@Test
	void normalizesCodeAndAllowsDocumentedTransitions() {
		Course draft = course(CourseStatus.DRAFT, 20);

		Course open = draft.transitionTo(CourseStatus.OPEN, CREATED_AT.plusSeconds(60));

		assertThat(open.courseCode()).isEqualTo("CS-101");
		assertThat(open.status()).isEqualTo(CourseStatus.OPEN);
		assertThat(open.acceptsEnrollment()).isTrue();
	}

	@Test
	void rejectsInvalidCourseValues() {
		assertThatThrownBy(() -> course(CourseStatus.DRAFT, 0))
				.isInstanceOf(DomainRuleViolationException.class)
				.hasMessage("capacity must be positive");
	}

	@Test
	void rejectsTransitionFromTerminalStatus() {
		Course cancelled = course(CourseStatus.CANCELLED, 20);

		assertThatThrownBy(() -> cancelled.transitionTo(CourseStatus.OPEN, CREATED_AT.plusSeconds(60)))
				.isInstanceOf(DomainRuleViolationException.class)
				.hasMessage("course cannot transition from CANCELLED to OPEN");
	}

	private static Course course(CourseStatus status, int capacity) {
		return new Course(UUID.randomUUID(), " cs-101 ", "Introduction", null, 3, capacity, status,
				UUID.randomUUID(), UUID.randomUUID(), CREATED_AT, CREATED_AT, 0);
	}
}
