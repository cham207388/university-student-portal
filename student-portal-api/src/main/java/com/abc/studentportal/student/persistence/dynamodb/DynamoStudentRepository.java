package com.abc.studentportal.student.persistence.dynamodb;

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
import com.abc.studentportal.student.application.DynamoStudentQueries;
import com.abc.studentportal.student.application.StudentRepository;
import com.abc.studentportal.student.domain.Student;
import com.abc.studentportal.student.domain.StudentStatus;

import java.util.Optional;
import java.util.UUID;

@DynamoPersistenceAdapter
public class DynamoStudentRepository extends AbstractDynamoRepository<Student, StudentDynamoRecord>
		implements StudentRepository, DynamoStudentQueries {
	private final DynamoCursorCodec cursorCodec;
	private final DynamoTransactionalWriter writer;
	public DynamoStudentRepository(DynamoDbTables tables, DynamoCursorCodec cursorCodec,
			DynamoTransactionalWriter writer) {
		super(tables.students(), "id", StudentDynamoMapper::toRecord, StudentDynamoMapper::toDomain,
				value -> value.id().toString());
		this.cursorCodec = cursorCodec;
		this.writer = writer;
	}
	@Override public Student create(Student value) {
		return StudentDynamoMapper.toDomain(writer.create(table(), "id", StudentDynamoMapper.toRecord(value), claims(value)));
	}
	@Override public Student update(Student value) {
		Student current = findById(value.id()).orElseThrow(() -> new com.abc.studentportal.common.exception.ConflictException(
				"Resource does not exist or was modified by another request"));
		return StudentDynamoMapper.toDomain(writer.update(table(), "id", StudentDynamoMapper.toRecord(value), value.version(),
				claims(current), claims(value)));
	}
	@Override public Optional<Student> findById(UUID id) { return findItem(id.toString()); }
	@Override public boolean existsByStudentNumber(String value) { return DynamoQueries.exists(table().index("students-by-number"), value); }
	@Override public boolean existsByEmail(String value) { return DynamoQueries.exists(table().index("students-by-email"), value); }
	@Override public void delete(Student value) {
		Student current = findById(value.id()).orElseThrow(() -> new com.abc.studentportal.common.exception.ConflictException(
				"Resource does not exist or was modified by another request"));
		writer.delete(table().tableName(), "id", value.id().toString(), value.version(), claims(current));
	}
	@Override public CursorPage<Student> findAll(CursorRequest request) {
		return query("students-catalog", DynamoCursorQueries.equalTo("STUDENT"), request, "STUDENT");
	}
	@Override public CursorPage<Student> findByDepartment(UUID departmentId, String lastNamePrefix, CursorRequest request) {
		String partition = departmentId.toString();
		String prefix = lastNamePrefix == null || lastNamePrefix.isBlank() ? null
				: lastNamePrefix.trim().toUpperCase(java.util.Locale.ROOT);
		var condition = prefix == null ? DynamoCursorQueries.equalTo(partition)
				: DynamoCursorQueries.beginsWith(partition, prefix);
		return query("students-by-department", condition, request, partition, prefix);
	}
	@Override public CursorPage<Student> findByStatus(StudentStatus status, CursorRequest request) {
		return query("students-by-status", DynamoCursorQueries.equalTo(status.name()), request, status.name());
	}
	private CursorPage<Student> query(String index, software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional condition,
			CursorRequest request, String... parameters) {
		return DynamoCursorQueries.query(table().index(index), condition, request,
				DynamoCursorQueries.identity(table().tableName(), index, parameters), cursorCodec,
				StudentDynamoMapper::toDomain);
	}
	private static java.util.List<DynamoUniqueClaim> claims(Student value) {
		String owner = value.id().toString();
		return java.util.List.of(new DynamoUniqueClaim("UNIQUE#STUDENT_NUMBER#" + value.studentNumber(), owner),
				new DynamoUniqueClaim("UNIQUE#EMAIL#" + value.email(), owner));
	}
}
