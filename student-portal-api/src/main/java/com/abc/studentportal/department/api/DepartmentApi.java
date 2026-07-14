package com.abc.studentportal.department.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class DepartmentApi {

	private DepartmentApi() {
	}

	public record CreateRequest(
			@NotBlank @Size(max = 20) String code,
			@NotBlank @Size(max = 150) String name,
			@Size(max = 2000) String description) {
	}

	public record UpdateRequest(
			@NotBlank @Size(max = 20) String code,
			@NotBlank @Size(max = 150) String name,
			@Size(max = 2000) String description,
			@PositiveOrZero long version) {
	}

	public record Response(UUID id, String code, String name, String description,
			Instant createdAt, Instant updatedAt, long version) {
	}
}
