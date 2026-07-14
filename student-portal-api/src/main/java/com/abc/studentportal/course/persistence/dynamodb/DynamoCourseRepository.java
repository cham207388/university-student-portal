package com.abc.studentportal.course.persistence.dynamodb;

import com.abc.studentportal.common.exception.ConflictException;
import com.abc.studentportal.common.pagination.CursorPage;
import com.abc.studentportal.common.pagination.CursorRequest;
import com.abc.studentportal.common.persistence.dynamodb.DynamoCursorCodec;
import com.abc.studentportal.common.persistence.dynamodb.DynamoCursorQueries;
import com.abc.studentportal.common.persistence.dynamodb.AbstractDynamoRepository;
import com.abc.studentportal.common.persistence.dynamodb.DynamoDbTables;
import com.abc.studentportal.common.persistence.dynamodb.DynamoPersistenceAdapter;
import com.abc.studentportal.common.persistence.dynamodb.DynamoQueries;
import com.abc.studentportal.course.application.CourseRepository;
import com.abc.studentportal.course.application.DynamoCourseQueries;
import com.abc.studentportal.course.domain.Course;
import com.abc.studentportal.course.domain.CourseStatus;

import java.util.Optional;
import java.util.UUID;

@DynamoPersistenceAdapter
public class DynamoCourseRepository extends AbstractDynamoRepository<Course, CourseDynamoRecord>
		implements CourseRepository, DynamoCourseQueries {
	private final DynamoCursorCodec cursorCodec;
	public DynamoCourseRepository(DynamoDbTables tables, DynamoCursorCodec cursorCodec) {
		super(tables.courses(), "id", CourseDynamoMapper::toRecord, CourseDynamoMapper::toDomain,
				value -> value.id().toString());
		this.cursorCodec = cursorCodec;
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
	@Override public CursorPage<Course> findAll(CursorRequest request) { return query("courses-catalog", "COURSE", request); }
	@Override public CursorPage<Course> findByDepartment(UUID id, CursorRequest request) {
		return query("courses-by-department", id.toString(), request);
	}
	@Override public CursorPage<Course> findByInstructor(UUID id, CursorRequest request) {
		return query("courses-by-instructor", id.toString(), request);
	}
	@Override public CursorPage<Course> findByStatus(CourseStatus status, CursorRequest request) {
		return query("courses-by-status", status.name(), request);
	}
	private CursorPage<Course> query(String index, String partition, CursorRequest request) {
		return DynamoCursorQueries.query(table().index(index), DynamoCursorQueries.equalTo(partition), request,
				DynamoCursorQueries.identity(table().tableName(), index, partition), cursorCodec,
				CourseDynamoMapper::toDomain);
	}
}
