package com.abc.studentportal.common.api;

import com.abc.studentportal.enrollment.api.EnrollmentApi;
import com.abc.studentportal.enrollment.domain.EnrollmentStatus;
import com.abc.studentportal.student.api.StudentApi;
import com.abc.studentportal.student.domain.StudentStatus;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApiValidationTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void validatesRequiredStudentFieldsAndEmail() {
		StudentApi.CreateRequest request = new StudentApi.CreateRequest("", "Ada", "Lovelace", "not-an-email",
				StudentStatus.ACTIVE, UUID.randomUUID());

		assertThat(validator.validate(request))
				.extracting(violation -> violation.getPropertyPath().toString())
				.containsExactlyInAnyOrder("studentNumber", "email");
	}

	@Test
	void validatesEnrollmentStatusAndGradeTogether() {
		EnrollmentApi.StatusRequest request = new EnrollmentApi.StatusRequest(EnrollmentStatus.DROPPED, "A", 0);

		assertThat(validator.validate(request))
				.singleElement()
				.satisfies(violation -> assertThat(violation.getMessage())
						.isEqualTo("finalGrade is only valid when status is COMPLETED"));
	}
}
