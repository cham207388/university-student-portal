package com.abc.studentportal.instructor.persistence.dynamodb;

import com.abc.studentportal.common.persistence.dynamodb.AbstractDynamoRepository;
import com.abc.studentportal.common.persistence.dynamodb.DynamoDbTables;
import com.abc.studentportal.common.persistence.dynamodb.DynamoPersistenceAdapter;
import com.abc.studentportal.common.persistence.dynamodb.DynamoQueries;
import com.abc.studentportal.instructor.application.InstructorRepository;
import com.abc.studentportal.instructor.domain.Instructor;

import java.util.Optional;
import java.util.UUID;

@DynamoPersistenceAdapter
public class DynamoInstructorRepository extends AbstractDynamoRepository<Instructor, InstructorDynamoRecord>
		implements InstructorRepository {
	public DynamoInstructorRepository(DynamoDbTables tables) {
		super(tables.instructors(), "id", InstructorDynamoMapper::toRecord, InstructorDynamoMapper::toDomain,
				value -> value.id().toString());
	}
	@Override public Instructor create(Instructor value) { return createItem(value); }
	@Override public Instructor update(Instructor value) { return updateItem(value); }
	@Override public Optional<Instructor> findById(UUID id) { return findItem(id.toString()); }
	@Override public boolean existsByEmployeeNumber(String value) { return DynamoQueries.exists(table().index("instructors-by-number"), value); }
	@Override public boolean existsByEmail(String value) { return DynamoQueries.exists(table().index("instructors-by-email"), value); }
	@Override public void delete(Instructor value) { deleteItem(value, value.version()); }
}
