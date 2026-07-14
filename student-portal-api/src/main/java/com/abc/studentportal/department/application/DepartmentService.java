package com.abc.studentportal.department.application;

import com.abc.studentportal.common.exception.ResourceNotFoundException;
import com.abc.studentportal.department.domain.Department;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@Profile({"local-dynamodb", "test-dynamodb"})
public class DepartmentService {
	private final DepartmentRepository repository;
	private final Clock clock;

	public DepartmentService(DepartmentRepository repository, Clock clock) {
		this.repository = repository; this.clock = clock;
	}

	public Department create(CreateCommand command) {
		Instant now = clock.instant();
		return repository.create(new Department(UUID.randomUUID(), command.code(), command.name(), command.description(),
				now, now, 0));
	}

	public Department update(UUID id, UpdateCommand command) {
		Department current = get(id);
		return repository.update(new Department(id, command.code(), command.name(), command.description(),
				current.createdAt(), clock.instant(), command.version()));
	}

	public Department get(UUID id) {
		return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Department", id));
	}

	public record CreateCommand(String code, String name, String description) { }
	public record UpdateCommand(String code, String name, String description, long version) { }
}
