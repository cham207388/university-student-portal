package com.abc.studentportal.course.application;

import com.abc.studentportal.course.domain.CourseStatus;

import java.util.UUID;

public record CourseFilter(UUID departmentId, UUID instructorId, CourseStatus status, String courseCode,
                           String title, Integer minimumCredits, Integer maximumCredits) {

    public CourseFilter {

        if (minimumCredits != null && minimumCredits <= 0) {
            throw new IllegalArgumentException("minimumCredits must be positive");
        }
        if (maximumCredits != null && maximumCredits <= 0) {
            throw new IllegalArgumentException("maximumCredits must be positive");
        }
        if (minimumCredits != null && maximumCredits != null && minimumCredits > maximumCredits) {
            throw new IllegalArgumentException("minimumCredits must not exceed maximumCredits");
        }
    }

}
