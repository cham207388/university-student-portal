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
import com.abc.studentportal.common.persistence.dynamodb.DynamoTransactionalWriter;
import com.abc.studentportal.common.persistence.dynamodb.DynamoUniqueClaim;
import com.abc.studentportal.common.persistence.dynamodb.DynamoRelationshipCounters;
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
	private final DynamoTransactionalWriter writer;
	private final DynamoRelationshipCounters counters;
	public DynamoCourseRepository(DynamoDbTables tables, DynamoCursorCodec cursorCodec,
			DynamoTransactionalWriter writer, DynamoRelationshipCounters counters) {
		super(tables.courses(), "id", CourseDynamoMapper::toRecord, CourseDynamoMapper::toDomain,
				value -> value.id().toString());
		this.cursorCodec = cursorCodec;
		this.writer = writer;
		this.counters = counters;
	}
	@Override public Course create(Course value) {
		return CourseDynamoMapper.toDomain(writer.create(table(), "id", CourseDynamoMapper.toRecord(value),
				java.util.List.of(claim(value)), java.util.List.of(
				counters.department(value.departmentId().toString(), "courseCount", 1),
				counters.instructor(value.instructorId().toString(), 1))));
	}
	@Override
	public Course update(Course value) {
		CourseDynamoRecord current = table().getItem(request -> request.key(key(value.id().toString())).consistentRead(true));
		if (current == null) {
			throw new ConflictException("Resource does not exist or was modified by another request");
		}
		CourseDynamoRecord record = CourseDynamoMapper.toRecord(value);
		record.setOccupiedSeats(current.getOccupiedSeats());
		record.setEnrollmentCount(current.getEnrollmentCount());
		Course old = CourseDynamoMapper.toDomain(current);
		java.util.List<software.amazon.awssdk.services.dynamodb.model.TransactWriteItem> moves = new java.util.ArrayList<>();
		if (!old.departmentId().equals(value.departmentId())) {
			moves.add(counters.department(old.departmentId().toString(), "courseCount", -1));
			moves.add(counters.department(value.departmentId().toString(), "courseCount", 1));
		}
		if (!old.instructorId().equals(value.instructorId())) {
			moves.add(counters.instructor(old.instructorId().toString(), -1));
			moves.add(counters.instructor(value.instructorId().toString(), 1));
		}
		return CourseDynamoMapper.toDomain(writer.update(table(), "id", record, value.version(),
				java.util.List.of(claim(old)), java.util.List.of(claim(value)), moves));
	}
	@Override public Optional<Course> findById(UUID id) { return findItem(id.toString()); }
	@Override public boolean existsByCourseCode(String value) { return DynamoQueries.exists(table().index("courses-by-code"), value); }
	@Override public void delete(Course value) {
		Course current = findById(value.id()).orElseThrow(() -> new ConflictException(
				"Resource does not exist or was modified by another request"));
		writer.delete(table().tableName(), "id", value.id().toString(), value.version(), java.util.List.of(claim(current)),
				java.util.List.of(counters.department(current.departmentId().toString(), "courseCount", -1),
						counters.instructor(current.instructorId().toString(), -1)), true);
	}
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
	private static DynamoUniqueClaim claim(Course value) {
		return new DynamoUniqueClaim("UNIQUE#COURSE_CODE#" + value.courseCode(), value.id().toString());
	}
}
