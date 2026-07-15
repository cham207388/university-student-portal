package com.abc.studentportal.course.domain;

import com.abc.studentportal.common.domain.DomainChecks;
import com.abc.studentportal.common.domain.DomainRuleViolationException;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record Course(UUID id, String courseCode, String title, String description, int credits, int capacity,
                     CourseStatus status, UUID departmentId, UUID instructorId, Instant createdAt, Instant updatedAt,
                     long version) {

    private static final Map<CourseStatus, Set<CourseStatus>> ALLOWED_TRANSITIONS = Map.of(
            CourseStatus.DRAFT, Set.of(CourseStatus.OPEN, CourseStatus.CANCELLED),
            CourseStatus.OPEN, Set.of(CourseStatus.CLOSED, CourseStatus.CANCELLED, CourseStatus.COMPLETED),
            CourseStatus.CLOSED, Set.of(CourseStatus.OPEN, CourseStatus.CANCELLED, CourseStatus.COMPLETED),
            CourseStatus.CANCELLED, Set.of(),
            CourseStatus.COMPLETED, Set.of());

    public Course {

        DomainChecks.audit(id, createdAt, updatedAt, version);
        courseCode = DomainChecks.uppercaseCode(courseCode, "courseCode");
        title = DomainChecks.requiredText(title, "title");
        description = DomainChecks.optionalText(description);
        DomainChecks.positive(credits, "credits");
        DomainChecks.positive(capacity, "capacity");
        DomainChecks.required(status, "status");
        DomainChecks.required(departmentId, "departmentId");
        DomainChecks.required(instructorId, "instructorId");
    }

    public boolean acceptsEnrollment() {

        return status == CourseStatus.OPEN;
    }

    public Course transitionTo(CourseStatus target, Instant changedAt) {

        DomainChecks.required(target, "status");
        if (target == status) {
            return this;
        }
        if (!ALLOWED_TRANSITIONS.get(status).contains(target)) {
            throw new DomainRuleViolationException("course cannot transition from " + status + " to " + target);
        }
        return new Course(id, courseCode, title, description, credits, capacity, target, departmentId,
                instructorId, createdAt, DomainChecks.required(changedAt, "changedAt"), version);
    }

}
