package com.abc.studentportal.enrollment.application;

import com.abc.studentportal.enrollment.domain.EnrollmentStatus;

import java.time.Instant;
import java.util.UUID;

public record EnrollmentFilter(UUID studentId, UUID courseId, EnrollmentStatus status,
                               Instant enrolledFrom, Instant enrolledTo) {

    public EnrollmentFilter {

        if (enrolledFrom != null && enrolledTo != null && enrolledFrom.isAfter(enrolledTo)) {
            throw new IllegalArgumentException("enrolledFrom must not be after enrolledTo");
        }
    }

}
