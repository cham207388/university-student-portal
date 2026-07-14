package com.abc.studentportal.department.persistence.dynamodb;

import com.abc.studentportal.common.persistence.dynamodb.AbstractDynamoRepository;
import com.abc.studentportal.common.persistence.dynamodb.DynamoDbTables;
import com.abc.studentportal.common.persistence.dynamodb.DynamoPersistenceAdapter;
import com.abc.studentportal.common.persistence.dynamodb.DynamoQueries;
import com.abc.studentportal.common.persistence.dynamodb.DynamoCursorCodec;
import com.abc.studentportal.common.persistence.dynamodb.DynamoCursorQueries;
import com.abc.studentportal.common.persistence.dynamodb.DynamoTransactionalWriter;
import com.abc.studentportal.common.persistence.dynamodb.DynamoUniqueClaim;
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
	private final DynamoTransactionalWriter writer;

	public DynamoDepartmentRepository(DynamoDbTables tables, DynamoCursorCodec cursorCodec,
			DynamoTransactionalWriter writer) {
		super(tables.departments(), "id", DepartmentDynamoMapper::toRecord, DepartmentDynamoMapper::toDomain,
				value -> value.id().toString());
		this.cursorCodec = cursorCodec;
		this.writer = writer;
	}

	@Override
	public Department create(Department value) {
		return DepartmentDynamoMapper.toDomain(writer.create(table(), "id", DepartmentDynamoMapper.toRecord(value),
				java.util.List.of(claim(value))));
	}

	@Override
	public Department update(Department value) {
		DepartmentDynamoRecord currentRecord = table()
				.getItem(request -> request.key(key(value.id().toString())).consistentRead(true));
		if (currentRecord == null)
			throw new com.abc.studentportal.common.exception.ConflictException(
					"Resource does not exist or was modified by another request");
		Department current = DepartmentDynamoMapper.toDomain(currentRecord);
		DepartmentDynamoRecord next = DepartmentDynamoMapper.toRecord(value);
		next.setStudentCount(currentRecord.getStudentCount());
		next.setInstructorCount(currentRecord.getInstructorCount());
		next.setCourseCount(currentRecord.getCourseCount());
		return DepartmentDynamoMapper.toDomain(writer.update(table(), "id", next,
				value.version(), java.util.List.of(claim(current)), java.util.List.of(claim(value))));
	}

	@Override
	public Optional<Department> findById(UUID id) {
		return findItem(id.toString());
	}

	@Override
	public boolean existsByCode(String code) {
		return DynamoQueries.exists(table().index("departments-by-code"), code);
	}

	@Override
	public Optional<Department> findByCode(String code) {
		return DynamoQueries.findOne(table().index("departments-by-code"), code)
				.map(DepartmentDynamoMapper::toDomain);
	}

	@Override
	public void delete(Department value) {
		Department current = findById(value.id())
				.orElseThrow(() -> new com.abc.studentportal.common.exception.ConflictException(
						"Resource does not exist or was modified by another request"));
		writer.deleteRequiringZeroCounters(table().tableName(), "id", value.id().toString(), value.version(),
				java.util.List.of(claim(current)), java.util.List.of(),
				java.util.List.of("studentCount", "instructorCount", "courseCount"));
	}

	@Override
	public CursorPage<Department> findAll(CursorRequest request) {
		String index = "departments-catalog";
		return DynamoCursorQueries.query(table().index(index), DynamoCursorQueries.equalTo("DEPARTMENT"), request,
				DynamoCursorQueries.identity(table().tableName(), index, "DEPARTMENT"), cursorCodec,
				DepartmentDynamoMapper::toDomain);
	}

	private static DynamoUniqueClaim claim(Department value) {
		return new DynamoUniqueClaim("UNIQUE#CODE#" + value.code(), value.id().toString());
	}
}
