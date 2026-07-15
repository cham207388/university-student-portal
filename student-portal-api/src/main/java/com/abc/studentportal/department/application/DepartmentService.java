package com.abc.studentportal.department.application;

import com.abc.studentportal.common.exception.ResourceNotFoundException;
import com.abc.studentportal.common.exception.ConflictException;
import com.abc.studentportal.common.application.DependencyChecker;
import com.abc.studentportal.department.domain.Department;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.Optional;

@Service
@Profile({"local-dynamodb", "test-dynamodb"})
public class DepartmentService {

    private final DepartmentRepository repository;

    private final Clock clock;

    private final DependencyChecker dependencies;

    public DepartmentService(DepartmentRepository repository, Clock clock, DependencyChecker dependencies) {
        this.repository = repository;
        this.clock = clock;
        this.dependencies = dependencies;
    }

    public Department create(CreateCommand command) {
        Instant now = clock.instant();
        return repository
                .create(new Department(UUID.randomUUID(), command.code(), command.name(), command.description(),
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

    public Optional<Department> findByCode(String code) {
        return repository.findByCode(com.abc.studentportal.common.domain.DomainChecks.uppercaseCode(code, "code"));
    }

    public void delete(UUID id, long version) {
        Department current = get(id);
        if (dependencies.departmentHasDependents(id))
            throw new ConflictException("Department still has dependent records");
        repository.delete(new Department(current.id(), current.code(), current.name(), current.description(),
                current.createdAt(), current.updatedAt(), version));
    }

    public record CreateCommand(String code, String name, String description) {

    }

    public record UpdateCommand(String code, String name, String description, long version) {

    }

}
