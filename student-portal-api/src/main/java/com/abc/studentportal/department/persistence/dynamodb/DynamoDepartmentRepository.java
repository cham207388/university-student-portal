package com.abc.studentportal.department.persistence.dynamodb;

import com.abc.studentportal.common.persistence.dynamodb.AbstractDynamoRepository;
import com.abc.studentportal.common.persistence.dynamodb.DynamoDbTables;
import com.abc.studentportal.common.persistence.dynamodb.DynamoPersistenceAdapter;
import com.abc.studentportal.common.persistence.dynamodb.DynamoQueries;
import com.abc.studentportal.common.persistence.dynamodb.DynamoCursorCodec;
import com.abc.studentportal.common.persistence.dynamodb.DynamoCursorQueries;
import com.abc.studentportal.common.pagination.CursorPage;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.department.application.DynamoDepartmentQueries;
import com.abc.studentportal.department.application.DepartmentRepository;
import com.abc.studentportal.department.domain.Department;

import java.util.Optional;
import java.util.UUID;

@DynamoPersistenceAdapter
public class DynamoDepartmentRepository extends AbstractDynamoRepository<Department, DepartmentDynamoRecord>
		implements DepartmentRepository, DynamoDepartmentQueries {
	private final DynamoCursorCodec cursorCodec;

	public DynamoDepartmentRepository(DynamoDbTables tables, DynamoCursorCodec cursorCodec) {
		super(tables.departments(), "id", DepartmentDynamoMapper::toRecord, DepartmentDynamoMapper::toDomain,
				value -> value.id().toString());
		this.cursorCodec = cursorCodec;
	}

	@Override public Department create(Department value) { return createItem(value); }
	@Override public Department update(Department value) { return updateItem(value); }
	@Override public Optional<Department> findById(UUID id) { return findItem(id.toString()); }
	@Override public boolean existsByCode(String code) { return DynamoQueries.exists(table().index("departments-by-code"), code); }
	@Override public void delete(Department value) { deleteItem(value, value.version()); }
	@Override public CursorPage<Department> findAll(CursorRequest request) {
		String index = "departments-catalog";
		return DynamoCursorQueries.query(table().index(index), DynamoCursorQueries.equalTo("DEPARTMENT"), request,
				DynamoCursorQueries.identity(table().tableName(), index, "DEPARTMENT"), cursorCodec,
				DepartmentDynamoMapper::toDomain);
	}
}
