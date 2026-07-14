package com.abc.studentportal.enrollment.application;

import com.abc.studentportal.common.pagination.CursorPage;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.enrollment.domain.Enrollment;
import com.abc.studentportal.enrollment.domain.EnrollmentStatus;

import java.time.Instant;
import java.util.UUID;

public interface DynamoEnrollmentQueries {
	CursorPage<Enrollment> findAll(CursorRequest request);
	CursorPage<Enrollment> findByStudent(UUID studentId, Instant from, Instant to, CursorRequest request);
	CursorPage<Enrollment> findByCourse(UUID courseId, Instant from, Instant to, CursorRequest request);
	CursorPage<Enrollment> findByStatus(EnrollmentStatus status, CursorRequest request);
}
