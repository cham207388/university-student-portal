package com.abc.studentportal.course.persistence.dynamodb;

import com.abc.studentportal.common.exception.ConflictException;
import com.abc.studentportal.common.persistence.dynamodb.AbstractDynamoRepository;
import com.abc.studentportal.common.persistence.dynamodb.DynamoDbTables;
import com.abc.studentportal.common.persistence.dynamodb.DynamoPersistenceAdapter;
import com.abc.studentportal.common.persistence.dynamodb.DynamoQueries;
import com.abc.studentportal.course.application.CourseRepository;
import com.abc.studentportal.course.domain.Course;

import java.util.Optional;
import java.util.UUID;

@DynamoPersistenceAdapter
public class DynamoCourseRepository extends AbstractDynamoRepository<Course, CourseDynamoRecord> implements CourseRepository {
	public DynamoCourseRepository(DynamoDbTables tables) {
		super(tables.courses(), "id", CourseDynamoMapper::toRecord, CourseDynamoMapper::toDomain,
				value -> value.id().toString());
	}
	@Override public Course create(Course value) { return createItem(value); }
	@Override
	public Course update(Course value) {
		CourseDynamoRecord current = table().getItem(request -> request.key(key(value.id().toString())).consistentRead(true));
		if (current == null) {
			throw new ConflictException("Resource does not exist or was modified by another request");
		}
		CourseDynamoRecord record = CourseDynamoMapper.toRecord(value);
		record.setOccupiedSeats(current.getOccupiedSeats());
		return updateRecord(record);
	}
	@Override public Optional<Course> findById(UUID id) { return findItem(id.toString()); }
	@Override public boolean existsByCourseCode(String value) { return DynamoQueries.exists(table().index("courses-by-code"), value); }
	@Override public void delete(Course value) { deleteItem(value, value.version()); }
}
