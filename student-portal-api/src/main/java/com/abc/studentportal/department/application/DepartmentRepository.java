package com.abc.studentportal.department.application;

import com.abc.studentportal.department.domain.Department;

import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository {

	Department save(Department department);

	Optional<Department> findById(UUID id);

	boolean existsByCode(String code);

	void delete(Department department);
}
