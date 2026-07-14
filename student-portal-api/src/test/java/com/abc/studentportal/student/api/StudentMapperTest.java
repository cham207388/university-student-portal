package com.abc.studentportal.student.api;

import com.abc.studentportal.student.domain.Student;
import com.abc.studentportal.student.domain.StudentStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StudentMapperTest {

	@Test
	void mapsDomainStudentToImmutableApiResponse() {
		UUID id = UUID.randomUUID();
		UUID departmentId = UUID.randomUUID();
		Instant now = Instant.parse("2026-01-01T00:00:00Z");
		Student student = new Student(id, "S-1", "Ada", "Lovelace", "ada@example.test",
				StudentStatus.ACTIVE, departmentId, now, now, 3);

		StudentApi.Response response = StudentMapper.toResponse(student);

		assertThat(response.id()).isEqualTo(id);
		assertThat(response.departmentId()).isEqualTo(departmentId);
		assertThat(response.version()).isEqualTo(3);
	}
}
