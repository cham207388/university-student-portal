package com.abc.studentportal.enrollment.domain;

import com.abc.studentportal.common.domain.DomainChecks;
import com.abc.studentportal.common.domain.DomainRuleViolationException;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record Enrollment(UUID id, UUID studentId, UUID courseId, EnrollmentStatus status, Instant enrolledAt,
		Instant droppedAt, String finalGrade, Instant createdAt, Instant updatedAt, long version) {

	private static final Map<EnrollmentStatus, Set<EnrollmentStatus>> ALLOWED_TRANSITIONS = Map.of(
			EnrollmentStatus.ENROLLED, Set.of(EnrollmentStatus.DROPPED, EnrollmentStatus.COMPLETED),
			EnrollmentStatus.WAITLISTED, Set.of(EnrollmentStatus.ENROLLED, EnrollmentStatus.DROPPED),
			EnrollmentStatus.DROPPED, Set.of(),
			EnrollmentStatus.COMPLETED, Set.of());

	public Enrollment {
		DomainChecks.audit(id, createdAt, updatedAt, version);
		DomainChecks.required(studentId, "studentId");
		DomainChecks.required(courseId, "courseId");
		DomainChecks.required(status, "status");
		DomainChecks.required(enrolledAt, "enrolledAt");
		finalGrade = DomainChecks.optionalText(finalGrade);
		if (status == EnrollmentStatus.DROPPED && droppedAt == null) {
			throw new DomainRuleViolationException("droppedAt is required for a dropped enrollment");
		}
		if (status != EnrollmentStatus.DROPPED && droppedAt != null) {
			throw new DomainRuleViolationException("droppedAt is only valid for a dropped enrollment");
		}
		if (finalGrade != null && status != EnrollmentStatus.COMPLETED) {
			throw new DomainRuleViolationException("finalGrade is only valid for a completed enrollment");
		}
	}

	public boolean isActive() {
		return status == EnrollmentStatus.ENROLLED || status == EnrollmentStatus.WAITLISTED;
	}

	public boolean consumesCapacity() {
		return status == EnrollmentStatus.ENROLLED || status == EnrollmentStatus.COMPLETED;
	}

	public Enrollment transitionTo(EnrollmentStatus target, String grade, Instant changedAt) {
		DomainChecks.required(target, "status");
		DomainChecks.required(changedAt, "changedAt");
		if (target == status) {
			return this;
		}
		if (!ALLOWED_TRANSITIONS.get(status).contains(target)) {
			throw new DomainRuleViolationException("enrollment cannot transition from " + status + " to " + target);
		}
		Instant newDroppedAt = target == EnrollmentStatus.DROPPED ? changedAt : null;
		return new Enrollment(id, studentId, courseId, target, enrolledAt, newDroppedAt, grade,
				createdAt, changedAt, version);
	}
}
