package com.abc.studentportal.student.persistence.dynamodb;

import com.abc.studentportal.common.persistence.dynamodb.AbstractDynamoRepository;
import com.abc.studentportal.common.persistence.dynamodb.DynamoDbTables;
import com.abc.studentportal.common.persistence.dynamodb.DynamoPersistenceAdapter;
import com.abc.studentportal.common.persistence.dynamodb.DynamoQueries;
import com.abc.studentportal.student.application.StudentRepository;
import com.abc.studentportal.student.domain.Student;

import java.util.Optional;
import java.util.UUID;

@DynamoPersistenceAdapter
public class DynamoStudentRepository extends AbstractDynamoRepository<Student, StudentDynamoRecord> implements StudentRepository {
	public DynamoStudentRepository(DynamoDbTables tables) {
		super(tables.students(), "id", StudentDynamoMapper::toRecord, StudentDynamoMapper::toDomain,
				value -> value.id().toString());
	}
	@Override public Student create(Student value) { return createItem(value); }
	@Override public Student update(Student value) { return updateItem(value); }
	@Override public Optional<Student> findById(UUID id) { return findItem(id.toString()); }
	@Override public boolean existsByStudentNumber(String value) { return DynamoQueries.exists(table().index("students-by-number"), value); }
	@Override public boolean existsByEmail(String value) { return DynamoQueries.exists(table().index("students-by-email"), value); }
	@Override public void delete(Student value) { deleteItem(value, value.version()); }
}
