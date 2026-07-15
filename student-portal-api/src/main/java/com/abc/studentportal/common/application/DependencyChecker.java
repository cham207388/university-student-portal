package com.abc.studentportal.common.application;

import java.util.UUID;

public interface DependencyChecker {

    boolean departmentHasDependents(UUID departmentId);

    boolean studentHasEnrollmentHistory(UUID studentId);

    boolean instructorHasCourses(UUID instructorId);

    boolean courseHasEnrollmentHistory(UUID courseId);

}
