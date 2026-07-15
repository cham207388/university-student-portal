package com.abc.studentportal.enrollment.application;

import com.abc.studentportal.common.pagination.CursorPage;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.enrollment.domain.Enrollment;
import com.abc.studentportal.enrollment.domain.EnrollmentStatus;

import java.time.Instant;
import java.util.UUID;

public interface DynamoEnrollmentQueries extends EnrollmentQueries { }
