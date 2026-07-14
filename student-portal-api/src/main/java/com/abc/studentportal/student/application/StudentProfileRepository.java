package com.abc.studentportal.student.application;

import com.abc.studentportal.student.domain.StudentProfile;

import java.util.Optional;
import java.util.UUID;

public interface StudentProfileRepository {

	StudentProfile create(StudentProfile profile);

	StudentProfile update(StudentProfile profile);

	Optional<StudentProfile> findByStudentId(UUID studentId);

	void delete(StudentProfile profile);
}
