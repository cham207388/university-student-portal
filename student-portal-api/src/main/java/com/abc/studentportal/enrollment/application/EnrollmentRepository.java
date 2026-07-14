package com.abc.studentportal.enrollment.application;

import com.abc.studentportal.enrollment.domain.Enrollment;

import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepository {

	Optional<Enrollment> findById(UUID id);

	boolean existsActiveByStudentIdAndCourseId(UUID studentId, UUID courseId);

	boolean existsByStudentId(UUID studentId);

	boolean existsByCourseId(UUID courseId);
}
