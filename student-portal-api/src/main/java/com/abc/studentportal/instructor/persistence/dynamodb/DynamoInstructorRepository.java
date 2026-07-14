package com.abc.studentportal.instructor.persistence.dynamodb;

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
import com.abc.studentportal.instructor.application.DynamoInstructorQueries;
import com.abc.studentportal.instructor.application.InstructorRepository;
import com.abc.studentportal.instructor.domain.Instructor;

import java.util.Optional;
import java.util.UUID;

@DynamoPersistenceAdapter
public class DynamoInstructorRepository extends AbstractDynamoRepository<Instructor, InstructorDynamoRecord>
		implements InstructorRepository, DynamoInstructorQueries {
	private final DynamoCursorCodec cursorCodec;
	private final DynamoTransactionalWriter writer;
	public DynamoInstructorRepository(DynamoDbTables tables, DynamoCursorCodec cursorCodec,
			DynamoTransactionalWriter writer) {
		super(tables.instructors(), "id", InstructorDynamoMapper::toRecord, InstructorDynamoMapper::toDomain,
				value -> value.id().toString());
		this.cursorCodec = cursorCodec;
		this.writer = writer;
	}
	@Override public Instructor create(Instructor value) {
		return InstructorDynamoMapper.toDomain(writer.create(table(), "id", InstructorDynamoMapper.toRecord(value), claims(value)));
	}
	@Override public Instructor update(Instructor value) {
		Instructor current = findById(value.id()).orElseThrow(() -> new com.abc.studentportal.common.exception.ConflictException(
				"Resource does not exist or was modified by another request"));
		return InstructorDynamoMapper.toDomain(writer.update(table(), "id", InstructorDynamoMapper.toRecord(value),
				value.version(), claims(current), claims(value)));
	}
	@Override public Optional<Instructor> findById(UUID id) { return findItem(id.toString()); }
	@Override public boolean existsByEmployeeNumber(String value) { return DynamoQueries.exists(table().index("instructors-by-number"), value); }
	@Override public boolean existsByEmail(String value) { return DynamoQueries.exists(table().index("instructors-by-email"), value); }
	@Override public void delete(Instructor value) {
		Instructor current = findById(value.id()).orElseThrow(() -> new com.abc.studentportal.common.exception.ConflictException(
				"Resource does not exist or was modified by another request"));
		writer.delete(table().tableName(), "id", value.id().toString(), value.version(), claims(current));
	}
	@Override public CursorPage<Instructor> findAll(CursorRequest request) {
		return query("instructors-catalog", "INSTRUCTOR", request);
	}
	@Override public CursorPage<Instructor> findByDepartment(UUID departmentId, CursorRequest request) {
		return query("instructors-by-department", departmentId.toString(), request);
	}
	private CursorPage<Instructor> query(String index, String partition, CursorRequest request) {
		return DynamoCursorQueries.query(table().index(index), DynamoCursorQueries.equalTo(partition), request,
				DynamoCursorQueries.identity(table().tableName(), index, partition), cursorCodec,
				InstructorDynamoMapper::toDomain);
	}
	private static java.util.List<DynamoUniqueClaim> claims(Instructor value) {
		String owner = value.id().toString();
		return java.util.List.of(new DynamoUniqueClaim("UNIQUE#EMPLOYEE_NUMBER#" + value.employeeNumber(), owner),
				new DynamoUniqueClaim("UNIQUE#EMAIL#" + value.email(), owner));
	}
}
