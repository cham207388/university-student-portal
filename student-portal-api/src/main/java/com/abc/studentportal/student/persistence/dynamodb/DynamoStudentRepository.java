package com.abc.studentportal.student.persistence.dynamodb;

import com.abc.studentportal.common.persistence.dynamodb.AbstractDynamoRepository;
import com.abc.studentportal.common.persistence.dynamodb.DynamoDbTables;
import com.abc.studentportal.common.persistence.dynamodb.DynamoPersistenceAdapter;
import com.abc.studentportal.common.persistence.dynamodb.DynamoQueries;
import com.abc.studentportal.common.persistence.dynamodb.DynamoCursorCodec;
import com.abc.studentportal.common.persistence.dynamodb.DynamoCursorQueries;
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
	public DynamoStudentRepository(DynamoDbTables tables, DynamoCursorCodec cursorCodec) {
		super(tables.students(), "id", StudentDynamoMapper::toRecord, StudentDynamoMapper::toDomain,
				value -> value.id().toString());
		this.cursorCodec = cursorCodec;
	}
	@Override public Student create(Student value) { return createItem(value); }
	@Override public Student update(Student value) { return updateItem(value); }
	@Override public Optional<Student> findById(UUID id) { return findItem(id.toString()); }
	@Override public boolean existsByStudentNumber(String value) { return DynamoQueries.exists(table().index("students-by-number"), value); }
	@Override public boolean existsByEmail(String value) { return DynamoQueries.exists(table().index("students-by-email"), value); }
	@Override public void delete(Student value) { deleteItem(value, value.version()); }
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
}
