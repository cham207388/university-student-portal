package com.abc.studentportal.department.application;

import com.abc.studentportal.department.domain.Department;

import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository {

	Department create(Department department);

	Department update(Department department);

	Optional<Department> findById(UUID id);

	Optional<Department> findByCode(String code);

	boolean existsByCode(String code);

	void delete(Department department);
}
