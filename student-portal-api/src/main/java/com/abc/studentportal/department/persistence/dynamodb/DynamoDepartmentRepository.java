package com.abc.studentportal.department.persistence.dynamodb;

import com.abc.studentportal.common.persistence.dynamodb.AbstractDynamoRepository;
import com.abc.studentportal.common.persistence.dynamodb.DynamoDbTables;
import com.abc.studentportal.common.persistence.dynamodb.DynamoPersistenceAdapter;
import com.abc.studentportal.common.persistence.dynamodb.DynamoQueries;
import com.abc.studentportal.department.application.DepartmentRepository;
import com.abc.studentportal.department.domain.Department;

import java.util.Optional;
import java.util.UUID;

@DynamoPersistenceAdapter
public class DynamoDepartmentRepository extends AbstractDynamoRepository<Department, DepartmentDynamoRecord>
		implements DepartmentRepository {

	public DynamoDepartmentRepository(DynamoDbTables tables) {
		super(tables.departments(), "id", DepartmentDynamoMapper::toRecord, DepartmentDynamoMapper::toDomain,
				value -> value.id().toString());
	}

	@Override public Department create(Department value) { return createItem(value); }
	@Override public Department update(Department value) { return updateItem(value); }
	@Override public Optional<Department> findById(UUID id) { return findItem(id.toString()); }
	@Override public boolean existsByCode(String code) { return DynamoQueries.exists(table().index("departments-by-code"), code); }
	@Override public void delete(Department value) { deleteItem(value, value.version()); }
}
