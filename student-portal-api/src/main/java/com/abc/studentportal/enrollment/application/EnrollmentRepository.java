package com.abc.studentportal.enrollment.application;

import com.abc.studentportal.enrollment.domain.Enrollment;

import java.util.Optional;
import java.util.UUID;

public interface EnrollmentRepository {
	Enrollment create(Enrollment enrollment);

	Enrollment update(Enrollment enrollment);

	Optional<Enrollment> findById(UUID id);

	boolean existsActiveByStudentIdAndCourseId(UUID studentId, UUID courseId);

	boolean existsByStudentId(UUID studentId);

	boolean existsByCourseId(UUID courseId);

}
