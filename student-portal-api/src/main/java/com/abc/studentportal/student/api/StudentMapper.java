package com.abc.studentportal.student.api;

import com.abc.studentportal.student.domain.Student;
import com.abc.studentportal.student.domain.StudentProfile;

public final class StudentMapper {

	private StudentMapper() {
	}

	public static StudentApi.Response toResponse(Student student) {
		return new StudentApi.Response(student.id(), student.studentNumber(), student.firstName(),
				student.lastName(), student.email(), student.status(), student.departmentId(),
				student.createdAt(), student.updatedAt(), student.version());
	}

	public static StudentApi.ProfileResponse toResponse(StudentProfile profile) {
		return new StudentApi.ProfileResponse(profile.id(), profile.studentId(), profile.dateOfBirth(),
				profile.phoneNumber(), profile.addressLine1(), profile.addressLine2(), profile.city(), profile.state(),
				profile.postalCode(), profile.country(), profile.createdAt(), profile.updatedAt(), profile.version());
	}
}
